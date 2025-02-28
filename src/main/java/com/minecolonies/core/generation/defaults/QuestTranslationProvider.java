package com.minecolonies.core.generation.defaults;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.minecolonies.api.util.Log;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;

import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.minecolonies.api.quests.QuestParseConstant.*;
import static com.minecolonies.api.quests.registries.QuestRegistries.DIALOGUE_OBJECTIVE_ID;
import static com.minecolonies.api.util.constant.Constants.MOD_ID;
import static com.minecolonies.core.generation.DataGeneratorConstants.COLONY_QUESTS_DIR;
import static com.minecolonies.core.quests.QuestParsingConstants.*;

/**
 * Magic translator for quests.  This parses the existing quest JSON files and moves the dialogue elements to
 * translation resources, so that translations can be provided for them.
 *
 * This requires that the 'source' quests under src/main/resources/data/minecolonies/quests only contain en-US
 * text and do not already contain translation keys.
 */
public class QuestTranslationProvider implements DataProvider
{
    private final PackOutput packOutput;

    public QuestTranslationProvider(@NotNull final PackOutput packOutput)
    {
        this.packOutput = packOutput;
    }

    @NotNull
    @Override
    public String getName()
    {
        return "QuestTranslationProvider";
    }

    @NotNull
    @Override
    public CompletableFuture<?> run(@NotNull final CachedOutput cache)
    {
        final PackOutput.PathProvider questProvider = packOutput.createPathProvider(PackOutput.Target.DATA_PACK, COLONY_QUESTS_DIR);
        final List<CompletableFuture<?>> quests = new ArrayList<>();

        try (final PackResources pack = new PathPackResources(new PackLocationInfo("mod/" + MOD_ID + "/src", Component.empty(), PackSource.BUILT_IN, Optional.empty()), Path.of("..", "..", "src", "main", "resources")))
        {
            pack.listResources(PackType.SERVER_DATA, MOD_ID, COLONY_QUESTS_DIR, (questId, stream) ->
            {
                if (!questId.getPath().endsWith(".json"))
                {
                    return;
                }

                final ResourceLocation questPath = new ResourceLocation(questId.getNamespace(), questId.getPath().replace(COLONY_QUESTS_DIR + "/", "").replace(".json", ""));
                final String baseKey = questPath.getNamespace() + ".quests." + questPath.getPath().replace("/", ".");
                final JsonObject langJson = new JsonObject();

                quests.add(CompletableFuture.supplyAsync(() ->
                {
                    try
                    {
                        final JsonObject questJson;
                        try (final InputStreamReader reader = new InputStreamReader(stream.get()))
                        {
                            questJson = GsonHelper.parse(reader);
                        }

                        processQuest(langJson, baseKey, questJson);

                        return questJson;
                    }
                    catch (final Exception e)
                    {
                        Log.getLogger().error("Failed to process {}", questPath.toString(), e);
                        return null;
                    }
                }, Util.backgroundExecutor()).thenComposeAsync(json ->
                {
                    if (json != null)
                    {
                        return DataProvider.saveStable(cache, json, questProvider.json(questPath))
                                .thenApply(q -> langJson);
                    }
                    return CompletableFuture.completedFuture(null);
                }, Util.backgroundExecutor()));
            });
        }

        return CompletableFuture.allOf(quests.toArray(CompletableFuture[]::new))
                .thenComposeAsync(v -> saveLanguage(cache, quests.stream().map(q -> (JsonObject) q.join()).toList()), Util.backgroundExecutor());
    }

    @NotNull
    private CompletableFuture<?> saveLanguage(@NotNull final CachedOutput cache,
                                              @NotNull final List<JsonObject> langJsons)
    {
        final JsonObject langJson = new JsonObject();
        for (final JsonObject questLang : langJsons)
        {
            for (final Map.Entry<String, JsonElement> entry : questLang.entrySet())
            {
                langJson.add(entry.getKey(), entry.getValue());
            }
        }

        final PackOutput.PathProvider langProvider = packOutput.createPathProvider(PackOutput.Target.RESOURCE_PACK, "lang");
        final Path langFile = langProvider.file(new ResourceLocation(MOD_ID, "quests"), "json");
        return DataProvider.saveStable(cache, langJson, langFile);
    }

    private void processQuest(final JsonObject langJson, final String baseKey, final JsonObject json)
    {
        final String name = json.get(NAME).getAsString();
        langJson.addProperty(baseKey, name);
        json.addProperty(NAME, baseKey);

        int objectiveCount = 0;
        for (final JsonElement objectivesJson : json.get(QUEST_OBJECTIVES).getAsJsonArray())
        {
            final String objectiveKey = baseKey + ".obj" + objectiveCount;
            final JsonObject objective = objectivesJson.getAsJsonObject();
            processObjective(langJson, objectiveKey, objective);
            ++objectiveCount;
        }
    }

    private void processObjective(final JsonObject langJson, final String baseKey, final JsonObject json)
    {
        final ResourceLocation type = ResourceLocation.parse(json.get(TYPE).getAsString());
        if (type.equals(DIALOGUE_OBJECTIVE_ID))
        {
            langJson.addProperty(baseKey, json.get(TEXT_ID).getAsString());
            json.addProperty(TEXT_ID, baseKey);

            int answerCount = 0;
            for (final JsonElement answerJson : json.get(OPTIONS_ID).getAsJsonArray())
            {
                final String answerKey = baseKey + ".answer" + answerCount;
                langJson.addProperty(answerKey, answerJson.getAsJsonObject().get(ANSWER_ID).getAsString());
                answerJson.getAsJsonObject().addProperty(ANSWER_ID, answerKey);

                final JsonObject result = answerJson.getAsJsonObject().get(RESULT_ID).getAsJsonObject();
                processObjective(langJson, answerKey + ".reply", result);
                ++answerCount;
            }
        }
    }

}
