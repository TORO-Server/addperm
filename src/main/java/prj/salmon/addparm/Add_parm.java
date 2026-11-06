package prj.salmon.addparm;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class Add_parm extends JavaPlugin {
    private static webhooksender webhookSender;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        String webhookUrl = getConfig().getString("webhook-url", "");
        if (webhookUrl.isEmpty()) {
            getLogger().warning("Webhook URL が設定されていません！");
        } else {
            webhookSender = new webhooksender(webhookUrl);
        }

        registerCommands();
    }

    private void registerCommands() {
        getCommand("addbuilder").setExecutor(new PermissionCommand("builder", true));
        getCommand("delbuilder").setExecutor(new PermissionCommand("builder", false));
        getCommand("adddriver").setExecutor(new PermissionCommand("driver", true));
        getCommand("deldriver").setExecutor(new PermissionCommand("driver", false));
    }

    public static webhooksender getWebhookSender() {
        return webhookSender;
    }

    public static UUID resolveUUID(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(playerName);
        if (offline.hasPlayedBefore()) {
            return offline.getUniqueId();
        }

        FloodgateApi floodgate = FloodgateApi.getInstance();
        if (floodgate != null) {
            UUID floodgateUUID = floodgate.getUuidFor(playerName).join();
            if (floodgateUUID != null) {
                return floodgateUUID;
            }
        }

        return null;
    }

    private class PermissionCommand implements CommandExecutor {
        private final String group;
        private final boolean isAdd;

        PermissionCommand(String group, boolean isAdd) {
            this.group = group;
            this.isAdd = isAdd;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length != 1) {
                sender.sendMessage(String.format("使用方法: /%s <MCID>", label));
                return true;
            }

            String targetName = args[0];
            LuckPerms luckPerms;
            try {
                luckPerms = LuckPermsProvider.get();
            } catch (IllegalStateException e) {
                sender.sendMessage("LuckPerms API のロードに失敗しました。");
                return true;
            }

            UUID targetUUID = resolveUUID(targetName);
            if (targetUUID == null) {
                sender.sendMessage("指定されたプレイヤーは存在しません。");
                return true;
            }

            luckPerms.getUserManager().loadUser(targetUUID).thenAcceptAsync(user -> {
                if (user == null) {
                    scheduleSyncTask(() -> sender.sendMessage("指定されたプレイヤーのデータを取得できませんでした。"));
                    return;
                }

                String groupNode = "group." + group;
                if (isAdd) {
                    user.data().add(Node.builder(groupNode).build());
                } else {
                    user.data().clear(node -> node.getKey().equals(groupNode));
                }
                luckPerms.getUserManager().saveUser(user);

                String senderName = sender instanceof Player ? ((Player) sender).getName() : "Console";
                String action = isAdd ? "付与" : "剥奪";
                String role = group.equals("builder") ? "WE" : "Driver";
                String title = String.format("%s が %s に %sを%sしました", senderName, targetName, role, action);
                if (webhookSender != null) {
                    webhookSender.createThreadAndSendLog(title);
                }

                scheduleSyncTask(() -> sender.sendMessage(String.format("%s に %sを%sしました。", targetName, role, action)));
            });

            return true;
        }

        private void scheduleSyncTask(Runnable task) {
            Bukkit.getScheduler().runTask(Add_parm.this, task);
        }
    }
}