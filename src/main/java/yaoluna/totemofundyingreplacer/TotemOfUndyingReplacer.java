package yaoluna.totemofundyingreplacer;

import net.fabricmc.api.ModInitializer;

public class TotemOfUndyingReplacer implements ModInitializer {
	@Override
	public void onInitialize() {
        TotemOfUndyingReplacerConfigManager.loadOrCreate();
	}
}