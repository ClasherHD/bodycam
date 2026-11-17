# **3.0 UPDATE IS OUT NOW! THE BIGGEST UPDATE YET!**

This mod introduces a **Bodycam** ecosystem that allows you to spectate other players and Watch Points in real time, track targets across dimensions, protect your privacy, and uncover ancient relics.

<details>
<summary><h2>New Items, Blocks & Enchantments</h2></summary>

<details>
<summary><h3>Items</h3></summary>

- **Observation Spyglass:**\
![Observation Spyglass](https://cdn.modrinth.com/data/cached_images/f490035444e08b3a60c1d6b0c2fd252bc3a847fd.png)\
Right-click the spyglass to open a list of active players and Watch Points. Select a target from the menu to instantly view the world through their perspective.

- **Player Locator:**\
![Player Locator](https://cdn.modrinth.com/data/cached_images/89ac1b55ec9c9163b04dbc6c6b408e7273c1194c.png)\
Track down other players in real time! Right-click the compass to open a GUI and select a target player.

  <details>
  <summary>5 Dynamic Needle States</summary>

  - **Idle** *(Spins randomly when no target is locked)*\
    ![Player Locator](https://cdn.modrinth.com/data/cached_images/89ac1b55ec9c9163b04dbc6c6b408e7273c1194c.png)
  - **Active Tracking** *(Points directly to the target when in range)*\
    ![plsd](https://cdn.modrinth.com/data/cached_images/48fb43436a0fe9efa64feba53b96cbdc5f683eb5.png)
  - **Out of Range** *(Spins wildly when the player is too far away)*\
    ![ploor](https://cdn.modrinth.com/data/cached_images/2f83d735297036fee1bee20d2c9c3f9d71a94c84.png)
  - **Cross-Dimension** *(Points to the nearest portal/gateway to guide you to their dimension)*\
    ![plod](https://cdn.modrinth.com/data/cached_images/289192d16ab5faf8d8672586388aca4ab0bb2978.png)
  - **Jammed** *(Spins wildly if the target uses a Jammer to hide)*\
    ![plj](https://cdn.modrinth.com/data/cached_images/84b0048f20f22a646db53152d3f20236ce2ce91c.png)

  </details>

- **Observation Crystal:**\
![Observation Crystal](https://cdn.modrinth.com/data/cached_images/b52b1bdb4581bf7cfe79fcf2889142a32720938e.png)\
This crystal is strictly required as a core component to craft, charge, and fuel all bodycam items and blocks.

- **Jammer:**\
![Jammer](https://cdn.modrinth.com/data/cached_images/2c983042eb52e0fe94960a7b4604853883f1003e.png)\
Blocks incoming observation signals to prevent being watched (also works in **Curios** slots).

  <details>
  <summary>3 Dynamic Modes</summary>

  - **Off** *(Everyone can observe you)*\
    ![joff](https://cdn.modrinth.com/data/cached_images/6c73b7d838e1d056e0773e42d67ae98c4b287f9d.png)
  - **On** *(No one can observe you)*\
    ![jon](https://cdn.modrinth.com/data/cached_images/f074b4c3721c5e9a37349b35741c8dd92de33647.png)
  - **Limited** *(Observation range is limited, default: 500 blocks)*\
    ![jlimited](https://cdn.modrinth.com/data/cached_images/418d509181f0b07cb25eee23050ce8768fbcf2ea.png)

  </details>

- **Anonymizer:**\
![Anonymizer](https://cdn.modrinth.com/data/cached_images/3cb5b7ed5724ac82b079bd6d153e864368fbf11c.png)\
The perfect stealth tool. Hides your own observer status from others so you won't show up as Blue in other players menus (also works in **Curios** slots).

  <details>
  <summary>2 Dynamic Modes</summary>

  - **Off** *(Observer status visible)*\
    ![aoff](https://cdn.modrinth.com/data/cached_images/bccd65c73663ef94d2c79ebc744e708cb41576ad.png)
  - **On** *(Observer status hidden)*\
    ![aon](https://cdn.modrinth.com/data/cached_images/51ace479912514caeb6bbd4192bcc0fa6c44521f.png)

  </details>

- **Dimension Locator:**\
![Dimension Locator](https://cdn.modrinth.com/data/cached_images/cf31d3bf968b28434963339c53a15073d580ecfc.gif)\
Opens the player menu and displays the current dimension of every player on the server.

- **Portable Nether Portal:**\
![pnpl](https://cdn.modrinth.com/data/cached_images/d6d6995709f8854dac90b682145142344b23ce55.png)\
Right-click an active Nether Portal to bind/charge it (2 charges). Hold right-click to channel and teleport between the Overworld and the Nether, without spawning a physical portal (clears suffocating blocks and places Obsidian beneath your feet if needed).

- **Logger, Log Paper & Log Book:**\
![lb](https://cdn.modrinth.com/data/cached_images/edb447632fdfa551cba4c6266e7c95d409b175fb.png)![lp](https://cdn.modrinth.com/data/cached_images/23104efd79f5d2b3164dd4c86857d1c3297c5166.png)![logger](https://cdn.modrinth.com/data/cached_images/2ef1bc42c457907df2a425ba59fb4f3b0d01711b.png)\
Crafted empty at the Ancient Table and refueled with paper in a crafting table (max 8 durability). Right-click to start/stop recording who observes you onto a **Log Paper** (up to 5 entries with observer name, timestamp, distance, and dimension). Combine up to 10 Log Papers into a **Log Book** at a Cartography Table (Log Book in one hand, Paper on the other and then right-click the table, using shears instead of a Log Paper removes a page of the Log Book), and optionally lock it with a 4-digit PIN (Shift + Right-click).

- **The All Seeing Eye:**\
![tasyd](https://cdn.modrinth.com/data/cached_images/47a9105c2287a69a82c7eb07ced4cdcb05f3e812.png)![asy](https://cdn.modrinth.com/data/cached_images/61c05896164e7f5b84b7d5e7c1097b1cb7aea344.png)\
An ultra-rare, unblockable one-time-use relic. Found deactivated and awakened at the Ancient Table with a full crystal charge. Reveals every player on the server regardless of distance, dimension, Jammer, Anonymizer (including *True Anonymity*), or Watch Points.

- **Creeping Death:**\
![cd](https://cdn.modrinth.com/data/cached_images/25e6a7544541bd4df559f0a64937ab70f01dc78d.png)\
A dark relic forged at the Ancient Table from an activated *All Seeing Eye*, *Echo Shard*, and *Nether Star*. Marks a target in the same dimension (or Shift + Right-click to choose a custom assassin skin), triggering a 10 second Darkness & heartbeat warning before spawning a relentless 1,000 HP assassin. If slain or after 5 minutes without killing the target, an OSC Nuke Shot detonates at its location.

- **Cherry House Explorer Map:**\
![emch](https://cdn.modrinth.com/data/cached_images/b7a51af60ed826420cf3cd65faf0246e2ff6cb39.png)\
Obtained from Cartographers or Wandering Traders to locate the nearest secluded **Cherry House** structure.

</details>

<details>
<summary><h3>Blocks</h3></summary>

- **Observation Crystal Block:**\
![Observation Crystal Block](https://cdn.modrinth.com/data/cached_images/3a35ee5fc3e55f47b448e58c2e461ab11ddff30a.png)\
A compact storage and decorative block made of Observation Crystals (Light Level 5).

- **Hologram Block:**\
![Hologram Block](https://cdn.modrinth.com/data/cached_images/bc0aca315f4572054df9ad38691bb4128dc53b16.png)\
Allows starting an observation directly from the world. Sneak and right-click to open the menu, leaving behind a completely immortal **Hologram Dummy**.

- **Ancient Table:**\
![at](https://cdn.modrinth.com/data/cached_images/2c8bfa876ca20a4ee8c5585539bb23e7faa07531.png)\
A weird hybrid Smithing Table & fuel powered workstation. Fueled with *Observation Crystals*, it recharges depleted bodycam devices and crafts all items (not blocks).

- **Watch Point:**\
![wp](https://cdn.modrinth.com/data/cached_images/4ed45177e44538198cefb532ff3dd081961727ae.png)\
Right-click to spawn a stationary player dummy on top of the block (acts as a 360° camera you can switch to from the Bodycam menu). Shift + Right-click to rename it. Configurable per player and server wide limits.

- **Pedestal:**\
![pedestal](https://cdn.modrinth.com/data/cached_images/9c455581bb41ed2d81c87523a8b1900c5169932c.png)\
A lectern style station for a **Jammer** or **Anonymizer** (including their enchantments). Charge with up to 10 *Observation Crystals* (+10 blocks per crystal, up maximum to a 100 block distance sphere) and power with Redstone to create a stationary protection zone. Outputs a Comparator signal (1 to 10).

</details>

<details>
<summary><h3>Enchantments</h3></summary>

![Reach Enchantment Book](https://cdn.modrinth.com/data/cached_images/204463bb22ec9f601937187da16fb1db3a4c4863.gif)

- **Reach:**
  <details>
  <summary>2 Levels</summary>

  - **Reach I:** Removes the distance limit within the same dimension.
  - **Reach II:** Removes the distance limit and works across all dimensions.

  </details>

- **Nightspy (I to IV):** *(Observation Spyglass only)*
  <details>
  <summary>4 Levels</summary>

  - **Nightspy I:** Grants Night Vision while observing (max 500 blocks, same dimension, night only).
  - **Nightspy II:** Unlimited range (same dimension, night only).
  - **Nightspy III:** Unlimited range, works day & night (same dimension).
  - **Nightspy IV:** Unlimited range, works day & night across all dimensions.

  </details>

- **Anonymity & True Anonymity:** *(Anonymizer only)*
  - **Anonymity:** Scrambles your displayed dimension on the *Dimension Locator* with a fixed length obfuscated string.
  - **True Anonymity:** Completely removes you from other players selection menus (ultra rare find in Cherry Houses / Scholar trades).

</details>

</details>

<details>
<summary><h2>Durability & Repair System</h2></summary>

Unlike normal Minecraft tools, Bodycam devices **never break permanently**. At `0` durability, they play a break sound, appear depleted/disabled, and lose their function until recharged. **Mending** cannot be applied to any Bodycam device, and **Creative Mode** consumes no durability.

- **Two Durability Types:**
  - **Per Use Devices** *(incompatible with Unbreaking)*:
    - **Observation Spyglass:** `16` max durability (loses `1` per observation).
    - **Dimension Locator:** `8` max durability (loses `1` per scan).
    - **Logger:** `8` max durability (loses `1` per recording; refueled with **Paper** in a Crafting Table instead of the Ancient Table).
  - **Per Time Devices** *(compatible with Unbreaking)*:
    - **Player Locator:** `560` max durability (loses `1` per active second).
    - **Anonymizer:** `448` max durability (loses `1` per active second).
    - **Jammer:** `336` max durability (loses `1` per active second).

- **How Repairing & Recharging Works (Ancient Table):**
  - Newly crafted Bodycam devices from the Ancient Table start **empty (`0` durability)** and must be charged before first use.
  - **Fuel Tank:** Insert **Observation Crystals** into the fuel slot. The Ancient Table holds up to **5 crystals** (`560` fuel units total, `112` fuel units per crystal).
  - **Starting a Repair:** Place **1** damaged or empty device into any of the 3 input slots (with an empty output slot) and press the button.
  - **Per Time Devices (`Player Locator`, `Anonymizer`, `Jammer`):** `1` fuel unit restores `1` durability (`1` crystal = `112` durability, `28` seconds per crystal by default). Partial fuel units are used as needed, so a full recharge costs `5` crystals for the Player Locator, `4` for the Anonymizer, and `3` for the Jammer.
  - **Per Use Devices (`Observation Spyglass`, `Dimension Locator`):** `1` full Observation Crystal (`112` fuel units) restores `1` durability point (`28` seconds per crystal by default).
  - **All Seeing Eye:** Found deactivated; requires a **full 5 crystal tank** (`560` fuel units) at the Ancient Table to awaken (`28` seconds).

- **Armor Trim Protection (`Crystal Resonance`):**
  - Each piece of armor with the **Observation Crystal Trim** grants a `12.5%` chance (`50%` with a full 4/4 set) to prevent durability loss on both Per-Use and Per-Time devices (rolled before *Unbreaking*).

</details>

<details>
<summary><h2>Observation Crystal Armor Trim</h2></summary>

![Observation Crystal Trim](https://cdn.modrinth.com/data/cached_images/f9672112d949e14e6acc3572588b74beb6e74d27.png)\
Apply an Observation Crystal to your armor at a Smithing Table for a fresh **Glow in the Dark** look:\
![Glow in the Dark](https://cdn.modrinth.com/data/cached_images/ab109f2b127c563004a02076287c9c28f0b63e32.png)
- **Crystal Resonance:** `12.5%` chance per trimmed armor piece (`50%` with a full set) to prevent durability/charge loss when using bodycam items.
- **Espionage Sense (4/4 Full Set):** Plays a subtle audio alert only for you whenever someone starts observing you (except via *The All Seeing Eye*).

</details>

<details>
<summary><h2>World Generation: Cherry House & Scholar NPC</h2></summary>

- **Cherry House Structure:**\
![ch](https://cdn.modrinth.com/data/cached_images/c34fcd1154ead18911f456c562befe4181777128_0.webp)\
A rare structure spawning exclusively in `cherry_grove` biomes, featuring 12 loot chests across 3 tiers (*Common*, *Uncommon*, *Rare*, plus a `1 : 100,000` *Ultra Rare* roll) containing mod gear, books, and relics.

- **Scholar NPC:**\
![snpc](https://cdn.modrinth.com/data/cached_images/3687dded56d5dd3a75ac06cd602fa4284e755001.png)\
Spawns inside the Cherry House with 5 rarity variants (*Common* to *Ultra Rare*) and 5 trade tiers using *Observation Crystals*. Beware: slaying a Scholar triggers a **Creeping Death curse** against you!

</details>

<details>
<summary><h2>Dummies, Menus & Immersive Camera Overlay</h2></summary>

- **Dummies:**\
While spectating, a physical dummy of your character remains in the world:
  - **Spyglass Dummy:** Damage taken is applied directly to you. If it dies, you die.
  - **Hologram Dummy:** Completely immortal.
  - **Watch Point Dummy:** Stationary camera dummy placed on a Watch Point block.

- **Selection Menu (Observing & Watch Points):**\
![selection menu](https://cdn.modrinth.com/data/cached_images/892cec780f31eedfe90e236d622128d268f2a5ae.png)\
Switch between **Players** and **Watch Points**. Buttons are dynamically disabled when out of reach, jammed, or when the target is already observing you.
  - **Dynamic Status Colors:** Blue (Observing you) > Red (Jammed) > Orange (Other dimension with Reach I) > Yellow (Out of range) > White (Available).

- **Selection Menu (Player Locator):**\
![selection menu player locator 1](https://cdn.modrinth.com/data/cached_images/96857b71ccfd947cb0d4a1c3748aa874ff15c5aa.png)\
![deselection menu](https://cdn.modrinth.com/data/cached_images/af273fe4f5c7d0793caec1ba97f7e663e839e64d.png)

- **Immersive Camera Overlay & Damage Vignette:**\
![Kamera Overlay](https://cdn.modrinth.com/data/cached_images/0b83828808e8edfc4853786b3862d0ce92cab073.png)\
Hides the crosshair, displays REC/Health/Exit overlays, and features a smooth **Damage Vignette** with signal flicker, static crackle, and instability warnings when the observed player takes heavy damage. Press sneak to safely return.

</details>

<details>
<summary><h2>Commands, Configs & Compatibility</h2></summary>

- **Commands (`/bodycam`):**
  - `/bodycam config`: Opens the in-game server configuration GUI
  - `/bodycam reload`: Reloads server config and syncs clients
  - `/bodycam reset [<player>]`: Emergency reset for stuck camera sessions
  - `/bodycam status [<player>]`: Shows observation, jammer, and anonymizer status
  - `/bodycam admin watchpoint <list | remove | tp>`: Manage active Watch Points
  - `/bodycam admin give <item> <players>`: Give any of the 20 mod items
  - `/bodycam admin tp <dummy | cam>`: Teleport dummies or jump to camera positions
  - `/bodycam admin remove <dummy | all dummies>`: Remove active player dummies

- **Config System (Client & Server GUI Windows):**\
![client_config](https://cdn.modrinth.com/data/cached_images/29e3a51abc8c4860c2d34e715ec4b228cfd32f6d.png)\
![server_config](https://cdn.modrinth.com/data/cached_images/0f768d365f8a40df7b62829bef00be46e6f2941b.png)\
Both Client and Server configs now open in clean, darkened GUI windows. Toggle individual overlays (including the *Damage Vignette*), customize all GUI colors, adjust distances/Watch Point limits, or enable/disable any mod item or feature.

- **Compatibility & Advancements:**
  - **Simple Voice Chat:** Hear audio as if you were standing at the observed player's location.
  - **Curios API:** Equip *Jammer* and *Anonymizer* in Curios slots.
  - **30 Advancements:** Dedicated Bodycam advancement tab including 3 hidden secret challenges.

</details>

<details>
<summary><h2>Recipes</h2></summary>

<details>
<summary><h3>Items</h3></summary>

- **Observation Spyglass:**\
![ossr](https://cdn.modrinth.com/data/cached_images/6d78a653bd956603ce19914ab583361c606e2eb1.png)


- **Player Locator:**\
![plr](https://cdn.modrinth.com/data/cached_images/4c517e66fc3816dca1416ce41433c3fc81eba4da.png)


- **Observation Crystal:**\
![ocr](https://cdn.modrinth.com/data/cached_images/64addb364cfb80a8f74667b772e375abf8478c3f.png)


- **Jammer:**\
![jr](https://cdn.modrinth.com/data/cached_images/285787ec7d834502cf3718fea9228cad8932297f.png)


- **Anonymizer:**\
![ar](https://cdn.modrinth.com/data/cached_images/44125e70f397336b1cb1dc8bf7d9c6225dc2bbb3.png)

- **Dimension Locator:**\
![dlr](https://cdn.modrinth.com/data/cached_images/6b5ab3ac9f400df37cc2636354a853e6a8a888e5.png)

- **Portable Nether Portal:**\
![pnpr](https://cdn.modrinth.com/data/cached_images/48b6ba1ae104aeac78c98a51ec53d059b5f6ca00.png)

- **Logger**\
![lr](https://cdn.modrinth.com/data/cached_images/a89529058b0d2e6b35709ed091793b9901f5e9fe.png)

- **Log Book:**\
![lbr](https://cdn.modrinth.com/data/cached_images/dda109c043ee804b4c210cc6582f5296585dad7a.png)

- **The All Seeing Eye:**\
![asyr](https://cdn.modrinth.com/data/cached_images/b67cedae2e4a97eaf77ae75d882777ee14759920.png)

- **Creeping Death:**\
![cdr](https://cdn.modrinth.com/data/cached_images/fd037016381cd4cf334c0806b1b4fb1514f2eb2b.png)

</details>

<details>
<summary><h3>Blocks</h3></summary>

- **Observation Crystal Block:**\
![ocbr](https://cdn.modrinth.com/data/cached_images/209144c49a6c8051d85d0ae750334f84eae47ad9.png)![ocbr2](https://cdn.modrinth.com/data/cached_images/ce7f158123c3e76edc4b8f089e5c2756e8108e74.png)

- **Hologram Block:**\
![hbr](https://cdn.modrinth.com/data/cached_images/244f42b11e18076a18e87d41477a837d89b54313.png)

- **Ancient Table:**\
![atr](https://cdn.modrinth.com/data/cached_images/370f90c42d4ba409870e9c82f0a05e4795e78630.png)

- **Watch Point:**\
![wpr](https://cdn.modrinth.com/data/cached_images/b4856cd34b45f88e936f16f7e05d87dd13399474.png)

- **Pedestal:**\
![pr](https://cdn.modrinth.com/data/cached_images/53b8d59a5557d4270d4301ca02c3d43ec64e1a62.png)

</details>

<details>
<summary><h3>Reach</h3></summary>

![rr](https://cdn.modrinth.com/data/cached_images/93314c9a0ddb2dc549369d6a07d6bdeda299a699.png)

</details>

</details>

</details>

<details>
<summary>Legacy Documentation (v2.1.2)</summary>

This mod introduces a Bodycam Monitor item that allows you to spectate other players in real-time.
## New Items & Blocks
- **Monitor Compass:**\
![Player Locator](https://cdn.modrinth.com/data/cached_images/89ac1b55ec9c9163b04dbc6c6b408e7273c1194c.png)\
Right-click the monitor to open a list of active players. Simply select a name from the menu to instantly view the world through their perspective


- **Reach Enchantment:**\
  ![Reach Enchantment Book](https://cdn.modrinth.com/data/cached_images/204463bb22ec9f601937187da16fb1db3a4c4863.gif)\
The standard Monitor Compass has a limited range.\
Apply the custom Reach enchantment to bypass the Monitor Compass distance limit and connect to players even across different dimensions


- **Observation Crystal:**\
![Observation Crystal](https://cdn.modrinth.com/data/cached_images/b52b1bdb4581bf7cfe79fcf2889142a32720938e.png)\
This crystal is strictly required as a core component to craft all bodycam items and blocks

- **Jammer:**\
![Jammer](https://cdn.modrinth.com/data/cached_images/2c983042eb52e0fe94960a7b4604853883f1003e.png)\
Blocks signals to prevent being observed\
Features 3 dynamic modes:
  - 🔴 *Off* (everyone can observe you)
  - 🟢 *On* (no one can observe you)
  - 🔵 *Limited* (observation range is limited (std: 500 Blocks))

- **Anonymizer:**\
![Anonymizer](https://cdn.modrinth.com/data/cached_images/3cb5b7ed5724ac82b079bd6d153e864368fbf11c.png)\
The perfect stealth tool. Hides your own observer status from others. If you have this item active, you won't show up as Blue in other players' menus, but will remain an inconspicuous White or Red\
Features 2 modes:
  - 🔴 *Off*
  - 🟢 *On*

- **Dimension Locator:**\
![Dimension Locator](https://cdn.modrinth.com/data/cached_images/cf31d3bf968b28434963339c53a15073d580ecfc.gif)\
Opens the player menu and displays the current dimension of every player on the server. Names are properly localized

- **Hologram Block:**\
![Hologram Block](https://cdn.modrinth.com/data/cached_images/bc0aca315f4572054df9ad38691bb4128dc53b16.png)\
Allows starting an observation directly from the world. Just sneak on it, right-click, and the menu opens. This leaves behind a special **Hologram Dummy** that is completely immortal and cannot be attacked

## Dummy
While you are spectating, a physical dummy of your character remains exactly where you left it, keeping your inventory and armor in the world.\
This features 2 variants:
  - **Compass Dummy:**
  Any damage the dummy takes is applied directly to you. If your dummy dies, you die as well. Your Items will be at the Dummys death location
  - **Hologram Dummy:**
  Completely immortal

## Simple Voice Chat Support
This mod is fully compatible with Simple Voice Chat. When you are in the camera view, hearing will behave as if you are at the location of the player you are spectating

## Selection Menu (observing)
Interacting with the **Monitor Compass** or **Hologram Block** will open a selection menu window with smooth scrolling. It will show all players on the server with name and player face:\
![selection menu](https://cdn.modrinth.com/data/cached_images/892cec780f31eedfe90e236d622128d268f2a5ae.png)

* **Smart Button System:** Observation buttons are dynamically grayed out when:
  * The target is out of range (over 500 blocks) or in another dimension
  * The target has a **Jammer** set to *Active* or *Limited* (and is out of range)
  * You are currently being observed by that specific person

* **Dynamic Status Colors:** Playernames in the menu are automatically color coded:
  * 🔵**Blue (Highest Priority):** Player is currently observing you
  * 🔴**Red (Second Highest Priority):** Player cannot be observed due to a Jammer
  * 🟡**Yellow (Third Highest Priority):** Player is out of reach (only when no reach)
  * ⚪**White (Standard):** Player can be observed normally

## Immersive Camera Overlay
Adds a slightly transparent, darkened frame around the edge of the screen heavily enhances the authentic camera aesthetic. HUD elements (name of the observed player, your health, and sneak status) are seamlessly integrated and clearly visible:\
![Kamera Overlay](https://cdn.modrinth.com/data/cached_images/0b83828808e8edfc4853786b3862d0ce92cab073.png)
* **Focus on Immersion:** The crosshair is strictly hidden during observation
- **Safe Return**: Simply press sneak to end the camera connection and safely return to your own body

## Command
  - `/camtp`: Teleports you to a dummy or player position

## Config System (Client & Server)
* **Ingame Client Config:** Players can now adjust their own settings directly in the game via the mod menu (including a scroll menu and reset button). Here, individual camera overlays (Name, Health, Shift) can be specifically toggled on and off. Additionally, all GUI colors can be personalized
![Client Config](https://cdn.modrinth.com/data/cached_images/69def2453e6d8bc8eecb99845171da2bc7163be0.png)
* **Server Config (Host Only):** Full control for server admins. The maximum monitor distance is adjustable (Default: 500). All new items as well as the Reach enchantment can be completely disabled. Furthermore, there is an OP-Only mode that unlocks the mod functions exclusively for server operators
![server config](https://cdn.modrinth.com/data/cached_images/b9bb407a7ec10caf4b363ad0995709e7de8fea67.png)

</details>
