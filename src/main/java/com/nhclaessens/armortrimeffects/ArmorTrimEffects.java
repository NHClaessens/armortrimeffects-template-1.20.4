package com.nhclaessens.armortrimeffects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nhclaessens.armortrimeffects.config.SimpleConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class ArmorTrimEffects implements ModInitializer {
	public static final String MOD_ID = "armortrimeffects";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private final HashMap<Number, ItemStack[]> lastArmorSets = new HashMap<>();
	private JsonArray ARMOR_SETS = null;
	private final ArrayList<String> activeEffectStrings = new ArrayList<>();


	SimpleConfig CONFIG = SimpleConfig.of("ArmorTrimEffectsConfig").provider(this::provider).request();

	private String provider(String filename) {
		return "{\n" +
				"  \"armor_sets\": [\n" +
				"\n" +
				"  ]\n" +
				"}";
	}

	@Override
	public void onInitialize() {
		ARMOR_SETS = CONFIG.getOrDefaultJsonArray("armor_sets", (new JsonArray()));
		LOGGER.info(String.valueOf(ARMOR_SETS));

		ServerTickEvents.END_WORLD_TICK.register(this::checkArmor);
	}

	private void checkArmor(ServerWorld server) {
		for(ServerPlayerEntity player : server.getPlayers()) {
			int playerIndex = player.getId();
			ItemStack[] currentArmorSet = new ItemStack[]{
					player.getInventory().getArmorStack(0), // Helmet
					player.getInventory().getArmorStack(1), // Chestplate
					player.getInventory().getArmorStack(2), // Leggings
					player.getInventory().getArmorStack(3)  // Boots
			};

			ItemStack[] last = lastArmorSets.get(playerIndex);

			for (int i = 0; i < 4; i++) {
				if(last == null || !ItemStack.areEqual(last[i], currentArmorSet[i])) {
					// Armor has changed
					onArmorChanged(currentArmorSet, player);
				}
			}

			lastArmorSets.put(playerIndex, currentArmorSet);
		}
	}

	private void onArmorChanged(ItemStack[] currentArmorSet, ServerPlayerEntity player) {
		// 1. remove all previous effects
		// 2. check valid armor sets
		// 3. apply those effects

		// Remove active effects
		for(String currentEffect : activeEffectStrings) {
			player.removeStatusEffect(stringToEffect(currentEffect));
		}

		// Check all sets from config
		for(JsonElement set : ARMOR_SETS ) {
			JsonObject set2 = (JsonObject) set;

			if((set2.get("helmet") != null && !compareItemToString(currentArmorSet[3], set2.get("helmet")) || TrimDoesNotMatch(set2, currentArmorSet[3], "helmet"))) {
				continue;
			}
			if((set2.get("chestplate") != null && !compareItemToString(currentArmorSet[2], set2.get("chestplate")) || TrimDoesNotMatch(set2, currentArmorSet[2], "chestplate"))) {
				continue;
			}
			if((set2.get("leggings") != null && !compareItemToString(currentArmorSet[1], set2.get("leggings")) || TrimDoesNotMatch(set2, currentArmorSet[1], "leggings"))) {
				continue;
			}
			if((set2.get("boots") != null && !compareItemToString(currentArmorSet[0], set2.get("boots")) || TrimDoesNotMatch(set2, currentArmorSet[0], "boots"))) {
				continue;
			}

			// Match
			JsonArray effects = (JsonArray) set2.get("effects");
			final int DURATION = Integer.MAX_VALUE;

			// Apply all effect for the current set
			for(JsonElement element : effects) {
				JsonObject object  = (JsonObject) element;

				String effectString = object.get("effect").toString().replace("\"", "");
				LOGGER.info(effectString);

				StatusEffect effect = stringToEffect(effectString);

				if(effect == null) {
					player.sendMessage(Text.of(effectString + " is not a valid effect"));
					continue;
				}

				int level = Integer.parseInt(String.valueOf(object.get("level")));
				boolean ambient = jsonGetBoolean(object, "ambient");
				boolean showParticles = jsonGetBoolean(object, "show_particles");

				activeEffectStrings.add(effectString);

				StatusEffectInstance instance = new StatusEffectInstance(effect, DURATION, level, ambient, showParticles);

				player.addStatusEffect(instance);
			}
		}
	}

	private Boolean compareItemToString(ItemStack item, JsonElement string) {
		return string.toString().replace("\"", "").compareTo(item.getItem().toString()) == 0;
	}

	private boolean jsonGetBoolean(JsonObject object, String key) {
		JsonElement val = object.get(key);

		return val != null && val.toString().equals("true");
	}

	private NbtElement getTrim(ItemStack item) {
		if(item.hasNbt()) {
			NbtCompound nbt = item.getNbt();

            assert nbt != null;
            if(nbt.contains("Trim")) {
				return nbt.get("Trim");

			}
		}
		return null;
	}

	private boolean TrimDoesNotMatch(JsonObject object, ItemStack item, String piece) {
		JsonElement pattern = object.get(piece + "_pattern");
		JsonElement material = object.get(piece + "_material");

		// No trim specified, so all good
		if(pattern == null && material == null) return false;

		NbtCompound trim = (NbtCompound) getTrim(item);
		if(trim == null) return true;

		boolean match = true;
		if(pattern != null) {
			LOGGER.info(piece + " pattern: " + trim.getString("pattern"));
			match = ("minecraft:" + pattern).replace("\"", "").equals(trim.getString("pattern"));
		}
		if(material != null) {
			LOGGER.info(piece + " material: " + trim.getString("material"));
			match = ("minecraft:" + material).replace("\"", "").equals(trim.getString("material"));
		}

		return !match;
	}

	private StatusEffect stringToEffect(String string) {
		return switch (string) {
			case "speed" -> StatusEffects.SPEED;
			case "slowness" -> StatusEffects.SLOWNESS;
			case "haste" -> StatusEffects.HASTE;
			case "mining_fatigue" -> StatusEffects.MINING_FATIGUE;
			case "strength" -> StatusEffects.STRENGTH;
			case "instant_health" -> StatusEffects.INSTANT_HEALTH;
			case "instant_damage" -> StatusEffects.INSTANT_DAMAGE;
			case "jump_boost" -> StatusEffects.JUMP_BOOST;
			case "nausea" -> StatusEffects.NAUSEA;
			case "regeneration" -> StatusEffects.REGENERATION;
			case "resistance" -> StatusEffects.RESISTANCE;
			case "fire_resistance" -> StatusEffects.FIRE_RESISTANCE;
			case "water_breathing" -> StatusEffects.WATER_BREATHING;
			case "invisibility" -> StatusEffects.INVISIBILITY;
			case "blindness" -> StatusEffects.BLINDNESS;
			case "night_vision" -> StatusEffects.NIGHT_VISION;
			case "hunger" -> StatusEffects.HUNGER;
			case "weakness" -> StatusEffects.WEAKNESS;
			case "poison" -> StatusEffects.POISON;
			case "health_boost" -> StatusEffects.HEALTH_BOOST;
			case "absorption" -> StatusEffects.ABSORPTION;
			case "saturation" -> StatusEffects.SATURATION;
			case "glowing" -> StatusEffects.GLOWING;
			case "levitation" -> StatusEffects.LEVITATION;
			case "luck" -> StatusEffects.LUCK;
			case "unluck" -> StatusEffects.UNLUCK;
			case "conduit_power" -> StatusEffects.CONDUIT_POWER;
			case "dolphins_grace" -> StatusEffects.DOLPHINS_GRACE;
			case "bad_omen" -> StatusEffects.BAD_OMEN;
			case "hero_of_the_village" -> StatusEffects.HERO_OF_THE_VILLAGE;
			case "darkness" -> StatusEffects.DARKNESS;
			default -> null;
		};
	}
}