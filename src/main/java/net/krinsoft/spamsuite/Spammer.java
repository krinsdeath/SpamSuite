package net.krinsoft.spamsuite;

/**
 * @author krinsdeath
 */
public class Spammer {
    private SpamSuite plugin;
    private String name;
    private short duplicates = 0;
    private short lines = 0;
    private int kicks = 0;
    private long time = 0;
    private String message;

    public Spammer(SpamSuite instance, String player) {
        plugin = instance;
        name = player;
        kicks = plugin.getConfig().getInt("spammer." + player, 0);
        plugin.debug(name + " has been registered.");
    }

    private boolean kick() {
        kicks++;
        String cmd = plugin.getKickCommand();
        if (kicks >= plugin.getMaxKicks()) {
            cmd = plugin.getBanCommand();
        }
        cmd = cmd.replaceAll("\\[user\\]", name);
        cmd = cmd.replaceAll("\\[kicks\\]", "[" + kicks + "/" + plugin.getMaxKicks() + "]");
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        plugin.getConfig().set("spammers." + name, kicks);
        return true;
    }

    public boolean addLine(String msg) {
        if (plugin.getServer().getPlayer(name).hasPermission("spamsuite.exempt")) {
            return false;
        }
        if (message == null || message.equalsIgnoreCase(msg)) {
            duplicates++;
            plugin.debug(">> " + name + ": duplicate message - " + duplicates + "/" + plugin.getMaxDuplicates());
        } else {
            duplicates = 0;
        }
        lines++;
        message = msg;
        if (duplicates >= plugin.getMaxDuplicates()) {
            return kick();
        }
        if (System.currentTimeMillis() - time <= plugin.getTime()) {
            plugin.debug(">> " + name + ": spam line - " + lines + "/" + plugin.getLines());
            if (lines >= plugin.getLines()) {
                return kick();
            }
        } else {
            lines = 0;
            time = System.currentTimeMillis();
        }
        return false;
    }

}
