package com.nhclaessens.armortrimeffects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nhclaessens.armortrimeffects.config.SimpleConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;


public class ArmorTrimEffects implements ModInitializer {
	public static final String MOD_ID = "armortrimeffects";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private final HashMap<Number, ItemStack[]> lastArmorSets = new HashMap<>();
	private JsonArray ARMOR_SETS = null;
	private final ArrayList<String> activeEffectStrings = new ArrayList<>();
	private final boolean debug = false;


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
		if(debug) LOGGER.info(String.valueOf(ARMOR_SETS));

		ServerTickEvents.END_WORLD_TICK.register(this::checkArmor);
	}

	private void checkArmor(ServerWorld server) {
		for(ServerPlayerEntity player : server.getPlayers()) {
			int playerIndex = player.getId();
			ItemStack[] currentArmorSet = new ItemStack[]{
					player.getInventory().getArmorStack(0).copy(), // Helmet
					player.getInventory().getArmorStack(1).copy(), // Chestplate
					player.getInventory().getArmorStack(2).copy(), // Leggings
					player.getInventory().getArmorStack(3).copy(),  // Boots
			};


			ItemStack[] last = lastArmorSets.get(playerIndex);
			if(debug) LOGGER.info("Current armor: " + Arrays.toString(currentArmorSet));
			if(debug) LOGGER.info("Last armor: " + Arrays.toString(last));

			for (int i = 0; i < 4; i++) {
				if(last == null || !ItemStack.areEqual(last[i], currentArmorSet[i])) {
					if(debug) LOGGER.info("Armor changed!");
					lastArmorSets.put(playerIndex, currentArmorSet);
					onArmorChanged(currentArmorSet, player);
					break;
				}
			}

		}
	}

	private void onArmorChanged(ItemStack[] currentArmorSet, ServerPlayerEntity player) {
		// 1. remove all previous effects
		// 2. check valid armor sets
		// 3. apply those effects

		// Remove active effects
		for(String currentEffect : activeEffectStrings) {
			Optional<RegistryEntry.Reference<StatusEffect>> effect = stringToEffect(currentEffect);
			if(effect.isPresent()){
				player.removeStatusEffect(stringToEffect(currentEffect).get());
			}
		}

		// Check all sets from config
		for(JsonElement set : ARMOR_SETS ) {
			JsonObject set2 = (JsonObject) set;

			if((set2.get("helmet") != null && !compareItemToString(currentArmorSet[3], set2.get("helmet")) || !TrimMatches(set2, currentArmorSet[3], "helmet"))) {
				if(debug) LOGGER.info("Helmet does not match, required: " + set2.get("helmet") + " wearing: " + currentArmorSet[3].getItem().toString());
				continue;
			}
			if((set2.get("chestplate") != null && !compareItemToString(currentArmorSet[2], set2.get("chestplate")) || !TrimMatches(set2, currentArmorSet[2], "chestplate"))) {
				if(debug) LOGGER.info("Chestplate does not match, required: " + set2.get("chestplate") + " wearing: " + currentArmorSet[2].getItem().toString());
				continue;
			}
			if((set2.get("leggings") != null && !compareItemToString(currentArmorSet[1], set2.get("leggings")) || !TrimMatches(set2, currentArmorSet[1], "leggings"))) {
				if(debug) LOGGER.info("Leggings do not match, required: " + set2.get("leggings") + " wearing: " + currentArmorSet[1].getItem().toString());
				continue;
			}
			if((set2.get("boots") != null && !compareItemToString(currentArmorSet[0], set2.get("boots")) || !TrimMatches(set2, currentArmorSet[0], "boots"))) {
				if(debug) LOGGER.info("Boots do not match, required: " + set2.get("boots") + " wearing: " + currentArmorSet[0].getItem().toString());
				continue;
			}

			// Match
			if(debug) LOGGER.info("Apply effects from set: " + set2);
			JsonArray effects = (JsonArray) set2.get("effects");
			final int DURATION = Integer.MAX_VALUE;

			// Apply all effect for the current set
			for(JsonElement element : effects) {
				JsonObject object  = (JsonObject) element;

				String effectString = object.get("effect").toString().replace("\"", "");

				Optional<RegistryEntry.Reference<StatusEffect>> effect = stringToEffect(effectString);

				if(effect.isEmpty()) {
					player.sendMessage(Text.of(effectString + " is not a valid effect"));
					continue;
				}

				int level = Integer.parseInt(String.valueOf(object.get("level")));
				boolean ambient = jsonGetBoolean(object, "ambient");
				boolean showParticles = jsonGetBoolean(object, "show_particles");

				if(debug) LOGGER.info("APPLY EFFECT: " + effectString + " lvl " + level);


				activeEffectStrings.add(effectString);

				StatusEffectInstance instance = new StatusEffectInstance(effect.get(), DURATION, level, ambient, showParticles);

				player.addStatusEffect(instance);
			}
		}
	}

	private Boolean compareItemToString(ItemStack item, JsonElement string) {
		String value = string.toString().replace("\"", "");
		String itemString = item.getItem().toString();
		return value.compareToIgnoreCase(itemString) == 0 || ("minecraft:" + value).compareToIgnoreCase(itemString) == 0;
	}

	private boolean jsonGetBoolean(JsonObject object, String key) {
		JsonElement val = object.get(key);

		return val != null && val.toString().equals("true");
	}

	private ArmorTrim getTrim(ItemStack item) {
			ComponentMap nbt = item.getComponents();

            assert nbt != null;
            if(nbt.contains(DataComponentTypes.TRIM)) {
				return nbt.get(DataComponentTypes.TRIM);

			}
		return null;
	}

	private boolean TrimMatches(JsonObject object, ItemStack item, String piece) {
		JsonElement pattern = object.get(piece + "_pattern");
		JsonElement material = object.get(piece + "_material");

		// No trim specified, so all good
		if(pattern == null && material == null) return true;

		ArmorTrim trim = getTrim(item);
		if(trim == null) return false;

		boolean patternmatch = true, materialmatch = true;
		if(debug){
			LOGGER.info("Pattern is: " + trim.getPattern().getIdAsString());
			LOGGER.info("Material is: " + trim.getMaterial().getIdAsString());
		}
		if(pattern != null) {
			patternmatch = ("minecraft:" + pattern).replace("\"", "").equals(trim.getPattern().getIdAsString());
		}
		if(material != null) {
			materialmatch = ("minecraft:" + material).replace("\"", "").equals(trim.getMaterial().getIdAsString());
		}

		if(debug) LOGGER.info("Pattern match: " + patternmatch + "Material match: " + materialmatch);
		return patternmatch && materialmatch;
	}

	private Optional<RegistryEntry.Reference<StatusEffect>> stringToEffect(String string) {
		Identifier id = Identifier.tryParse(string);
		return Registries.STATUS_EFFECT.getEntry(id);

//		return switch (string) {
//			case "speed" -> StatusEffects.SPEED;
//			case "slowness" -> StatusEffects.SLOWNESS;
//			case "haste" -> StatusEffects.HASTE;
//			case "mining_fatigue" -> StatusEffects.MINING_FATIGUE;
//			case "strength" -> StatusEffects.STRENGTH;
//			case "instant_health" -> StatusEffects.INSTANT_HEALTH;
//			case "instant_damage" -> StatusEffects.INSTANT_DAMAGE;
//			case "jump_boost" -> StatusEffects.JUMP_BOOST;
//			case "nausea" -> StatusEffects.NAUSEA;
//			case "regeneration" -> StatusEffects.REGENERATION;
//			case "resistance" -> StatusEffects.RESISTANCE;
//			case "fire_resistance" -> StatusEffects.FIRE_RESISTANCE;
//			case "water_breathing" -> StatusEffects.WATER_BREATHING;
//			case "invisibility" -> StatusEffects.INVISIBILITY;
//			case "blindness" -> StatusEffects.BLINDNESS;
//			case "night_vision" -> StatusEffects.NIGHT_VISION;
//			case "hunger" -> StatusEffects.HUNGER;
//			case "weakness" -> StatusEffects.WEAKNESS;
//			case "poison" -> StatusEffects.POISON;
//			case "health_boost" -> StatusEffects.HEALTH_BOOST;
//			case "absorption" -> StatusEffects.ABSORPTION;
//			case "saturation" -> StatusEffects.SATURATION;
//			case "glowing" -> StatusEffects.GLOWING;
//			case "levitation" -> StatusEffects.LEVITATION;
//			case "luck" -> StatusEffects.LUCK;
//			case "unluck" -> StatusEffects.UNLUCK;
//			case "slow_falling" -> StatusEffects.SLOW_FALLING;
//			case "conduit_power" -> StatusEffects.CONDUIT_POWER;
//			case "dolphins_grace" -> StatusEffects.DOLPHINS_GRACE;
//			case "bad_omen" -> StatusEffects.BAD_OMEN;
//			case "hero_of_the_village" -> StatusEffects.HERO_OF_THE_VILLAGE;
//			case "darkness" -> StatusEffects.DARKNESS;
//			case "trial_omen" -> StatusEffects.TRIAL_OMEN;
//			case "raid_omen" -> StatusEffects.RAID_OMEN;
//			case "wind_charged" -> StatusEffects.WIND_CHARGED;
//			case "weaving" -> StatusEffects.WEAVING;
//			case "oozing" -> StatusEffects.OOZING;
//			case "infested" -> StatusEffects.INFESTED;
//			default -> null;
//		};
	}
}