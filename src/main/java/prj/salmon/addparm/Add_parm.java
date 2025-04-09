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
        saveDefaultConfig(); // config.ymlがない場合に自動生成

        String webhookUrl = getConfig().getString("webhook-url", "");
        if (webhookUrl.isEmpty()) {
            getLogger().warning("Webhook URL が設定されていません！");
        } else {
            webhookSender = new webhooksender(webhookUrl);
        }

        getCommand("addbuilder").setExecutor(new AddBuilderCommand());
        getCommand("delbuilder").setExecutor(new DelBuilderCommand());
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
            UUID floodgateUUID = floodgate.getUuidFor(playerName).join(); // 非同期結果を同期的に取得
            if (floodgateUUID != null) {
                return floodgateUUID;
            }
        }

        return null;
    }

    public static class AddBuilderCommand implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length != 1) {
                sender.sendMessage("使用方法: /addbuilder <MCID>");
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
                    Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(Add_parm.class), () ->
                            sender.sendMessage("指定されたプレイヤーのデータを取得できませんでした。")
                    );
                    return;
                }

                Node builderGroupNode = Node.builder("group.builder").build();
                user.data().add(builderGroupNode);
                luckPerms.getUserManager().saveUser(user);

                // 送信者がプレイヤーかコンソールかを判定
                String senderName = sender instanceof Player ? ((Player) sender).getName() : "Console";

                String title = senderName + " が " + targetName + " に WEを付与しました";
                webhookSender.createThreadAndSendLog(title);

                Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(Add_parm.class), () ->
                        sender.sendMessage(targetName + " に WEを付与しました。")
                );
            });

            return true;
        }
    }

    public static class DelBuilderCommand implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length != 1) {
                sender.sendMessage("使用方法: /delbuilder <MCID>");
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
                    Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(Add_parm.class), () ->
                            sender.sendMessage("指定されたプレイヤーのデータを取得できませんでした。")
                    );
                    return;
                }

                // group.builder を削除
                user.data().clear(node -> node.getKey().equals("group.builder"));
                luckPerms.getUserManager().saveUser(user);

                // 送信者がプレイヤーかコンソールかを判定
                String senderName = sender instanceof Player ? ((Player) sender).getName() : "Console";

                String title = senderName + " が " + targetName + " から WEを剥奪しました";
                webhookSender.createThreadAndSendLog(title);

                Bukkit.getScheduler().runTask(JavaPlugin.getProvidingPlugin(Add_parm.class), () ->
                        sender.sendMessage(targetName + " から WEを剥奪しました。")
                );
            });

            return true;
        }
    }
}
