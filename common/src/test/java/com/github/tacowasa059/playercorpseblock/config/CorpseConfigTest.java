package com.github.tacowasa059.playercorpseblock.config;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorpseConfigTest {

    @Test
    void usesTheDefaultsWhenNothingIsSet() {

        final CorpseConfig config = new CorpseConfig(new Properties());

        assertTrue(config.enabled());
        assertEquals(2, config.pileRadius());
        assertEquals(24, config.maxPileHeight());
        assertEquals(0, config.despawnSeconds());
        assertEquals(32, config.renderDistance());
        assertEquals(16, config.detailDistance());
        assertTrue(config.cullHiddenCorpses());
    }

    @Test
    void hiddenCorpseCullingCanBeTurnedOff() {

        final Properties properties = new Properties();
        properties.setProperty("cullHiddenCorpses", "false");

        assertFalse(new CorpseConfig(properties).cullHiddenCorpses());
    }

    @Test
    void clampsValuesThatAreOutOfRange() {

        final Properties properties = new Properties();
        properties.setProperty("pileRadius", "99");
        properties.setProperty("maxPileHeight", "0");
        properties.setProperty("renderDistance", "-5");
        properties.setProperty("detailDistance", "999");
        properties.setProperty("despawnSeconds", "-1");

        final CorpseConfig config = new CorpseConfig(properties);

        assertEquals(8, config.pileRadius());
        assertEquals(1, config.maxPileHeight());
        assertEquals(8, config.renderDistance());
        assertEquals(256, config.detailDistance());
        assertEquals(0, config.despawnSeconds());
    }

    @Test
    void keepsValuesThatAreInRange() {

        final Properties properties = new Properties();
        properties.setProperty("pileRadius", "4");
        properties.setProperty("maxPileHeight", "12");
        properties.setProperty("renderDistance", "96");
        properties.setProperty("detailDistance", "40");
        properties.setProperty("despawnSeconds", "600");

        final CorpseConfig config = new CorpseConfig(properties);

        assertEquals(4, config.pileRadius());
        assertEquals(12, config.maxPileHeight());
        assertEquals(96, config.renderDistance());
        assertEquals(40, config.detailDistance());
        assertEquals(600, config.despawnSeconds());
    }

    @Test
    void fallsBackToTheDefaultWhenAValueIsNotANumber() {

        final Properties properties = new Properties();
        properties.setProperty("pileRadius", "wide");

        assertEquals(2, new CorpseConfig(properties).pileRadius());
    }

    @Test
    void readsBooleansAndIgnoresCaseAndWhitespace() {

        final Properties properties = new Properties();
        properties.setProperty("enabled", "  TRUE  ");

        assertTrue(new CorpseConfig(properties).enabled());
    }

    @Test
    void readsBooleansThatAreTurnedOff() {

        final Properties properties = new Properties();
        properties.setProperty("enabled", "false");

        assertFalse(new CorpseConfig(properties).enabled());
    }

    @Test
    void treatsAnUnreadableBooleanAsFalse() {

        final Properties properties = new Properties();
        properties.setProperty("enabled", "yes please");

        assertFalse(new CorpseConfig(properties).enabled());
    }

    @Test
    void ignoresWhitespaceAroundNumbers() {

        final Properties properties = new Properties();
        properties.setProperty("despawnSeconds", "  300  ");

        assertEquals(300, new CorpseConfig(properties).despawnSeconds());
    }
}
