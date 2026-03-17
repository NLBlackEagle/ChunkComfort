package chunkcomfort;

import java.util.Map;

import fermiumbooter.FermiumRegistryAPI;
import org.spongepowered.asm.launch.MixinBootstrap;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class ChunkComfortPlugin implements IFMLLoadingPlugin {

    public ChunkComfortPlugin() {
        MixinBootstrap.init();
        FermiumRegistryAPI.enqueueMixin(false, "mixins.chunkcomfort.vanilla.json");

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