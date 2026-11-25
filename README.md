# Pillage the Village

Pillage the Village adds passive Illager mercenaries and abandoned camps to Minecraft. Recruit a Mercenary with an emerald, give it a weapon, and it will help protect you on your travels.

## Contents

- [Mercenary](#mercenary)
- [Mercenary camp](#mercenary-camp)
- [Development](#development)

## Mercenary

<!-- Add the Mercenary and its biome variants image here. -->

| Property | Details |
| --- | --- |
| Family | Illager |
| Temperament | Passive |
| Health | 20 HP (10 hearts) |
| Recruitment item | Emerald |
| Combat styles | Melee, bow and crossbow |

The **Mercenary** is a peaceful member of the Illager family. It carries no weapon when encountered and does not attack unless it has been recruited and armed. Mercenaries do not flee from zombies, and zombies do not target them.

Mercenaries can open and close wooden doors, but do not follow a villager’s daily routine or seek out beds.

### Recruitment and equipment

Give an untamed Mercenary an emerald to recruit it. A recruited Mercenary can be equipped with swords, pickaxes, axes, hoes, bows or crossbows. If you replace its weapon, it drops the previous one. Its weapon is also dropped when it dies.

An armed Mercenary helps defend its owner. It attacks hostile creatures, avoids Creepers, and leaves Illagers and other allies alone unless its owner starts a fight with them.

### Biome variants

Mercenaries have different appearances based on the biome where they spawn. The image above shows their biome variants.

### Volunteer

The **Volunteer** is a special Mercenary variant. It may appear near a recruited Mercenary. Volunteer arrivals can be enabled or disabled with the volunteer_spawn gamerule, which is on by default.

## Mercenary camp

<!-- Add an image showing the Mercenary Camp and its biome variants here. -->

Mercenary camps are abandoned camps found across a variety of Overworld biomes:

**Plains, Desert, Savanna, Snowy Plains, Taiga, Meadow, Forest, Old Growth Birch Forest, Sparse Jungle, Bamboo Jungle, Swamp, Cherry Grove, Badlands, Eroded Badlands and Wooded Badlands.**

### Camp loot

Camp chests can contain the following items:

| Item | Minimum | Maximum |
| --- | ---: | ---: |
| Bow or crossbow | 0 | 1 |
| Copper or iron sword | 0 | 1 |
| Copper or iron axe | 0 | 1 |
| Goat horn (any of eight instrument variants) | 0 | 1 |
| Copper or iron ingots (combined) | 4 | 10 |
| Food: raw beef, raw mutton, raw porkchop, potato, carrot, apple or bread | 3 | 8 |
| Empty map | 0 | 1 |
| Bundle | 0 | 1 |
| Dark oak log | 4 | 12 |

Each chest selects exactly one weapon from the first three rows. The selected weapon may be enchanted. Amounts for ingots and food are totals across their listed item groups.

## Development

This repository contains separate NeoForge and Fabric projects, each with its own Gradle wrapper.

To build from the repository root on Windows:

```powershell
.\NeoForge\gradlew.bat build
.\Fabric\gradlew.bat build
```

On Linux or macOS:

```sh
./NeoForge/gradlew build
./Fabric/gradlew build
```

## License

The mod code is distributed under the GNU Lesser General Public License v3.0 only. See [LICENSE](LICENSE).