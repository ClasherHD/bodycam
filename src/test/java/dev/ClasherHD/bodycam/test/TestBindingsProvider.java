package dev.ClasherHD.bodycam.test;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.I18NParser;
import net.minecraftforge.fml.IBindingsProvider;
import net.minecraftforge.fml.config.IConfigEvent;

import java.util.function.Supplier;

public class TestBindingsProvider implements IBindingsProvider {

    @Override
    public Supplier<IEventBus> getForgeBusSupplier() {
        return () -> null;
    }

    @Override
    public Supplier<I18NParser> getMessageParser() {
        return () -> null;
    }

    @Override
    public Supplier<IConfigEvent.ConfigConfig> getConfigConfiguration() {
        return () -> null;
    }
}
