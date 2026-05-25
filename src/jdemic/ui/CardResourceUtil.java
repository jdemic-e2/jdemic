package jdemic.ui;

import jdemic.GameLogic.CityNode;
import jdemic.GameLogic.Card;

public final class CardResourceUtil {
    private CardResourceUtil() {
    }

    public static String colorPrefix(CityNode city) {
        return switch (city.getNativeColor()) {
            case BLUE -> "Blue";
            case YELLOW -> "Yellow";
            case BLACK -> "Green";
            case RED -> "Red";
        };
    }

    public static String cityResourceName(String cityName) {
        return switch (cityName) {
            case "Moscow" -> "Moskow";
            case "Tehran" -> "Teheran";
            default -> cityName.replace(" ", "").replace(".", "");
        };
    }

    public static String cityCardPath(CityNode city) {
        return "/cityCards/" + colorPrefix(city) + cityResourceName(city.getName()) + ".png";
    }

    public static String epidemicCardPath(CityNode city) {
        return "/epidemicCards/" + colorPrefix(city) + cityResourceName(city.getName()) + ".png";
    }

    public static String eventCardPath(Card card) {
        if (card == null) {
            return null;
        }

        if (card.getEventType() == null) {
            String fallbackName = card.getCardName();
            return fallbackName == null ? null : "/eventCards/Event" + fallbackName.replace(" ", "") + ".png";
        }

        String resourceName = switch (card.getEventType()) {
            case FIREWALL -> "FirewallLockdown";
            case SATELLITE -> "SatelliteOverride";
            case SERVER -> "ServerDeployment";
            case CONTROL -> "SystemControl";
            case THREAT -> "ThreatScan";
        };

        return "/eventCards/Event" + resourceName + ".png";
    }
}
