# ArmorTrimEffects
 Give a player status effects when they equip a certain set of armor

## Configuration

 Configuration is done in the `json.config` file, see example below

```json
{
  "armor_sets": [
    {
      "helmet": "netherite_helmet",
      "helmet_pattern": "rib",
      "helmet_material": "emerald",

      "chestplate": "diamond_chestplate",
      "leggings": "diamond_leggings",
      "boots": "diamond_boots",

      "effects": [
        {
          "effect": "speed",
          "level": 2,
          "show_particles": true,
          "ambient": true
        },
        {
          "effect": "night_vision",
          "level": 1,
          "show_particles": false
        }
      ]
    }
  ]
}
```
### Field explanation

| Field            | Mandatory | Possible values                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|------------------|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `armor_sets`     | Yes       | List of any size                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `helmet`         | No        | `leather_helmet`, `chainmail_helmet`,`iron_helmet`,`golden_helmet`,`diamond_helmet`,`netherite_helmet`, `turtle_helmet`, `carved_pumpkin`                                                                                                                                                                                                                                                                                                                      |
| `chestplate`     | No        | `leather_chestplate`, `chainmail_chestplate`,`iron_chestplate`,`golden_chestplate`,`diamond_chestplate`,`netherite_chestplate`                                                                                                                                                                                                                                                                                                                                 |
| `leggings`       | No        | `leather_leggings`, `chainmail_leggings`,`iron_leggings`,`golden_leggings`,`diamond_leggings`,`netherite_leggings`                                                                                                                                                                                                                                                                                                                                             |
| `boots`          | No        | `leather_boots`, `chainmail_boots`,`iron_boots`,`golden_boots`,`diamond_boots`,`netherite_boots`                                                                                                                                                                                                                                                                                                                                                               |
| `X_pattern`      | No        | `sentry`, `vex`, `wild`, `coast`, `dune`, `wayfinder`, `raiser`, `shaper`, `host`, `ward`, `silence`, `tide`, `snout`, `rib`, `eye`, `spire`                                                                                                                                                                                                                                                                                                                   |
| `X_material`     | No        | `iron`, `copper`, `gold`, `lapis`, `emerald`, `diamond`, `netherite`, `redstone`, `amethyst`, `quartz`                                                                                                                                                                                                                                                                                                                                                         |
| `effects`        | Yes       | List of any size                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `effect`         | Yes       | `speed`, `slowness`, `haste`, `mining_fatigue`, `strength`, `instant_health`, `instant_damage`, `jump_boost`, `nausea`, `regeneration`, `resistance`, `fire_resistance`, `water_breathing`, `invisibility`, `blindness`, `night_vision`, `hunger`, `weakness`, `poison`, `wither`, `health_boost`, `absorption`, `saturation`, `glowing`, `levitation`, `luck`, `unluck`, `slow_falling`, `conduit_power`, `dolphins_grace`, `bad_omen`, `hero_of_the_village` |
| `level`          | Yes       | A number from 1 to 255                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| `show_particles` | No        | `true` or `false`, defaults to `false` if omitted                                                                                                                                                                                                                                                                                                                                                                                                              |
| `ambient`        | No        | `true` or `false`, defaults to `false` if omitted                                                                                                                                                                                                                                                                                                                                                                                                              |

When a field is _not_ mandatory, that means you can leave it out all together, do **not** leave fields blank.

Fields that are left out act like wildcards, anything goes.