package chunkcomfort;

import fermiumbooter.FermiumRegistryAPI;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class ChunkComfortPlugin implements IFMLLoadingPlugin {

    public ChunkComfortPlugin() {
        MixinBootstrap.init();
        FermiumRegistryAPI.enqueueMixin(false, "mixins.chunkcomfort.vanilla.json");
        FermiumRegistryAPI.enqueueMixin(true, "mixins.chunkcomfort.dynsurroundhud.hidehud.json", () -> Loader.isModLoaded("dshuds"));
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {}

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}