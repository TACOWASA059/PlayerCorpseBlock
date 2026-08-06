package com.github.tacowasa059.playercorpseblock;

import com.github.tacowasa059.playercorpseblock.config.CorpseConfig;
import com.github.tacowasa059.playercorpseblock.platform.Services;

/**
 * Shared entry point. Both loaders register their content first and then call this.
 */
public final class PlayerCorpseBlockMod {

    private PlayerCorpseBlockMod() {
    }

    public static void init() {

        final CorpseConfig config = CorpseConfig.get();

        Constants.LOG.info("PlayerCorpseBlock is running on {} in a {} environment.",
                Services.PLATFORM.getPlatformName(), Services.PLATFORM.getEnvironmentName());
        Constants.LOG.debug("Corpses enabled: {}, pile radius: {}, max pile height: {}, despawn seconds: {}",
                config.enabled(), config.pileRadius(), config.maxPileHeight(), config.despawnSeconds());
    }
}
