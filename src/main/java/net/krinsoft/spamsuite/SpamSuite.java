package net.krinsoft.spamsuite;

import net.krinsoft.spamsuite.listeners.PlayerListener;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author krinsdeath
 */
public class SpamSuite extends JavaPlugin {
    private boolean debug;

    private String bcmd;
    private String rcmd;
    private String kcmd;
    private int lines;
    private int seconds;
    private int kicks;
    private int duplicates;

    private Map<String, Spammer> spammers = new HashMap<String, Spammer>();

    @Override
    public void onEnable() {
        long startup = System.currentTimeMillis();
        // register the spam guard
        spamGuard();

        // create listeners
        PlayerListener pListener = new PlayerListener(this);
        
        // register them
        getServer().getPluginManager().registerEvents(pListener, this);
        
        log("Plugin initialized. (" + (System.currentTimeMillis() - startup) + "ms)");
    }

    @Override
    public void onDisable() {
        // save configuration file
        saveConfig();

        // clear objects
        spammers.clear();
        
        // report done
        log("Plugin disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { return false; }
        String sub = subCommand(args[0]);
        if (command.getName().equalsIgnoreCase("spamsuite")) {
            if (!sender.hasPermission("spamsuite." + sub) && !(sender instanceof ConsoleCommandSender)) {
                return false;
            }
            if (sub.equalsIgnoreCase("debug")) {
                debug = !debug;
                message(sender, "Debug mode is now " + (debug ? "enabled" : "disabled") + ".");
                return true;
            } else if (sub.equalsIgnoreCase("reset") && args.length == 2) {
                String cmd = rcmd;
                OfflinePlayer player;
                if (getServer().getPlayer(args[1]) == null) {
                    if ((player = getServer().getOfflinePlayer(args[1])) == null) {
                        message(sender, "That player doesn't exist.");
                        return true;
                    }
                } else {
                    player = getServer().getPlayer(args[1]);
                }
                cmd = cmd.replaceAll("\\[user\\]", player.getName());
                message(sender, "Resetting user '" + player.getName() + "'...");
                getConfig().set("spammers." + player.getName(), 0);
                getServer().dispatchCommand(getServer().getConsoleSender(), cmd);
                return true;
            } else if (sub.equalsIgnoreCase("version")) {
                message(sender, "SpamSuite v" + this.getDescription().getVersion());
                message(sender, "by krinsdeath");
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    private String subCommand(String cmd) {
        if (cmd.equalsIgnoreCase("debug") || cmd.equalsIgnoreCase("-d")) {
            return "debug";
        } else if (cmd.equalsIgnoreCase("reset") || cmd.equalsIgnoreCase("-r")) {
            return "reset";
        } else {
            return "version";
        }
    }

    public void message(CommandSender sender, String message) {
        message = "[" + this + "] " + message;
        sender.sendMessage(message);
    }

    public void log(String message) {
        message = "[" + this + "] " + message;
        getServer().getLogger().info(message);
    }

    public void debug(String message) {
        if (debug) {
            message = "[" + this + "] [Debug] " + message;
            getServer().getLogger().info(message);
        }
    }

    public void spamGuard() {
        for (Player player : getServer().getOnlinePlayers()) {
            if (spammers.containsKey(player.getName())) { continue; }
            spammers.put(player.getName(), new Spammer(this, player.getName()));
        }
        getConfig().setDefaults(YamlConfiguration.loadConfiguration(this.getClass().getResourceAsStream("/config.yml")));
        if (!new File(getDataFolder(), "config.yml").exists()) {
            getConfig().options().copyDefaults(true);
        }
        this.debug = getConfig().getBoolean("debug", false);
        this.rcmd = getConfig().getString("reset_command", "pardon [user]");
        this.bcmd = getConfig().getString("ban_command", "ban [user] You have been banned for being excessively annoying.");
        this.kcmd = getConfig().getString("kick_command", "kick [user] Stop spamming. [kicks]");
        this.lines = getConfig().getInt("spam.lines", 3);
        this.seconds = getConfig().getInt("spam.seconds", 1);
        this.kicks = getConfig().getInt("spam.kicks", 3);
        this.duplicates = getConfig().getInt("spam.duplicates", 3);

        getConfig().options().header(
                "SpamSuite v" + this.getDescription().getVersion() + "\n" +
                "by krinsdeath\n" +
                "---\n" +
                "[user]: Evaluates to the offending player\n" +
                "[kicks]: Evaluates to the number of kicks / total kicks before ban for the specified user\n" +
                "---\n" +
                "debug: Toggles additional (debugger) messages in the server.log\n" +
                "---\n" +
                "reset_command: Specifies the command to execute when resetting a user's infractions\n" +
                "ban_command: Specifies the command to execute on the server when the kick threshold has been reached\n" +
                "kick_command: Specifies the command to execute on the server when the spam threshold has been reached\n" +
                "---\n" +
                "spam:\n" +
                "  lines: number of lines before the user is kicked (default is 3 lines of text)\n" +
                "  seconds: number of seconds allowed before max lines is met and user is kicked (default is 1)\n" +
                "  kicks: number of kicks before the user is banned (default is 3 kicks until a ban)\n" +
                "  duplicates: number of duplicate messages before user is kicked (default is 3)"
        );
        
        saveConfig();

    }

    public void unregister(String player) {
        debug(player + " has been unregistered.");
        spammers.remove(player);
    }
    
    public boolean addLine(String player, String message) {
        if (!spammers.containsKey(player)) {
            spammers.put(player, new Spammer(this, player));
        }
        return spammers.get(player).addLine(message);
    }

    public String getKickCommand() {
        return kcmd;
    }

    public String getBanCommand() {
        return bcmd;
    }

    public int getMaxKicks() {
        return kicks;
    }

    public int getLines() {
        return lines;
    }

    public int getTime() {
        return seconds * 1000;
    }

    public int getMaxDuplicates() {
        return duplicates;
    }

}
