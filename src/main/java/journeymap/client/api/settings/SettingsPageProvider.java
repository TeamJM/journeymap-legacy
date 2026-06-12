package journeymap.client.api.settings;

/**
 * Supplies a settings page that JourneyMap can display inside its options GUI.
 */
public interface SettingsPageProvider
{
    SettingsPageDefinition getPageDefinition();
}
