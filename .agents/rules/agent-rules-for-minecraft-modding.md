---
trigger: always_on
---

# Pragmatic SOLID Architecture for Minecraft Mods (Forge/Fabric/NeoForge)

This rule defines the architectural guidelines for Minecraft Mod development, focusing on stability, preventing sidedness crashes, and maintaining a clean separation of concerns.

## 1. Single Responsibility (SRP) & Folder Structure
Never create a "God Class". The main mod entry point (e.g. `ModName.java`) must be restricted to bootstrapping tasks: loading configuration, registering deferred registries, and setting up event buses. It must not contain gameplay, tick, or complex event-handling logic. While line count is not a rigid limit, it should remain minimal (typically under 150 lines). Concerns must be strictly separated using the following package structure:

* **`registry/`**: Contains only registry classes (e.g., `ModItems.java`, `ModBlocks.java`, `ModEntityTypes.java`).
  * *Rule*: No gameplay logic inside registries. All fields must be `public static final RegistryObject<T>`.
* **`event/`**: Contains event handlers.
  * *`ModEvents.java`*: Static class with `@Mod.EventBusSubscriber(bus = Bus.MOD)`. Handles only lifecycle events (CommonSetup, Entity Attributes).
  * *`ServerEvents.java`*: Static class with `@Mod.EventBusSubscriber(bus = Bus.FORGE)`. Handles gameplay ticking, commands registration, and server-side player events.
  * *`client/event/`*: Handles client-only rendering or GUI overlays. Must have `value = Dist.CLIENT`.
* **`network/`**: Contains packets (`Packet.java`) and the `PacketHandler.java`. Each packet class must only handle its own serialization, deserialization, and logical execution.
* **`commands/`**: Contains command registration classes.

---

## 2. Minecraft-specific SOLID Application

### Liskov Substitution Principle (LSP) - CRITICAL
When subclassing Minecraft base classes (e.g., `Item`, `Block`, `LivingEntity`, `Screen`):
* **Always preserve base behavior**: If you override lifecycle methods (such as `hurt()`, `die()`, `tick()`, `remove()`), you **MUST** call the superclass equivalent (`super.tick()`, etc.) unless you explicitly intend to block the engine's behavior. Failing to do so causes severe registry leaks and game crashes.

### Pragmatic Design (Ignore DIP & ISP Internally)
* Do **NOT** implement custom interfaces or Dependency Injection frameworks for internal mod items or systems. 
* Direct, static referencing of registry objects (e.g., `ModItems.MY_ITEM.get()`) is the platform standard and must be used to keep the codebase simple and maintainable.

---

## 3. Code Cleanliness & IDE Warnings
* **Unused Code**: Regularly clean up imports and unused private fields.
* **No Comments**: Do not write code comments (inline, block, or javadoc) unless they are explicitly requested or document highly complex, non-obvious algorithms. The code itself should be self-documenting.
* **Null Safety**: Forge/Minecraft projects often trigger false-positive null-pointer warnings in IDEs. Use `@SuppressWarnings("null")` at the class level on registries to keep the workspace warning-free without cluttering the code with redundant null checks.
