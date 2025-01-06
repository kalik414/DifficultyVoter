package dev.kali;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class dv extends JavaPlugin implements TabExecutor {

    private String ongoingVoteDifficulty = null;
    private final Map<Player, Boolean> votes = new HashMap<>();
    private double requiredVotePercentage;
    private int maxVoteDuration;
    private int voteCooldown;
    private final Map<UUID, Long> playerCooldowns = new HashMap<>();
    private boolean difficultyLocked = false;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        requiredVotePercentage = config.getDouble("vote-percentage", 0.6);
        voteCooldown = config.getInt("vote-cooldown", 300);
        maxVoteDuration = 30;
        getCommand("dv").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Commands are only available to players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /dv <easy|normal|hard> or /dv vote <yes|no> or /dv forcestop.");
            return true;
        }

        if (args[0].equalsIgnoreCase("forcestop")) {
            if (!player.hasPermission("dv.forcestop")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                return true;
            }

            if (ongoingVoteDifficulty == null) {
                player.sendMessage(ChatColor.YELLOW + "There is no active vote at the moment.");
                return true;
            }

            cancelVote();
            Bukkit.broadcastMessage(ChatColor.RED + "The vote has been canceled by an administrator.");
            return true;
        }

        if (args[0].equalsIgnoreCase("vote")) {
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /dv vote <yes|no>.");
                return true;
            }

            if (ongoingVoteDifficulty == null) {
                player.sendMessage(ChatColor.RED + "There is no active vote at the moment.");
                return true;
            }

            boolean vote;
            if (args[1].equalsIgnoreCase("yes")) {
                vote = true;
            } else if (args[1].equalsIgnoreCase("no")) {
                vote = false;
            } else {
                player.sendMessage(ChatColor.RED + "Invalid choice. Use: /dv vote <yes|no>.");
                return true;
            }

            handleVote(player, vote);
            return true;
        }

        if (ongoingVoteDifficulty != null || difficultyLocked) {
            player.sendMessage(ChatColor.RED + "A vote is already in progress, or difficulty is temporarily locked.");
            return true;
        }

        long currentTime = System.currentTimeMillis();
        if (!player.hasPermission("dv.bypasscooldown")) {
            Long lastVoteTime = playerCooldowns.get(player.getUniqueId());
            if (lastVoteTime != null && (currentTime - lastVoteTime) < voteCooldown * 1000L) {
                long timeLeft = (voteCooldown * 1000L - (currentTime - lastVoteTime)) / 1000;
                player.sendMessage(ChatColor.RED + "You can start a new vote in " + timeLeft + " seconds.");
                return true;
            }
        }

        String difficulty = args[0].toLowerCase();
        if (!difficulty.equals("easy") && !difficulty.equals("normal") && !difficulty.equals("hard")) {
            player.sendMessage(ChatColor.RED + "Invalid difficulty. Use: easy, normal, or hard.");
            return true;
        }

        startVote(player, difficulty);
        return true;
    }

    private void startVote(Player initiator, String difficulty) {
        ongoingVoteDifficulty = difficulty;
        votes.clear();
        playerCooldowns.put(initiator.getUniqueId(), System.currentTimeMillis());

        Bukkit.broadcastMessage(ChatColor.GREEN + initiator.getName() + " suggests changing the server difficulty to " + difficulty.toUpperCase() + ".");
        Bukkit.broadcastMessage(ChatColor.AQUA + "[Vote Yes] /dv vote yes [Vote No] /dv vote no");

        new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "The voting time has ended. Counting votes...");
                processVoteResults();
            }
        }.runTaskLater(this, maxVoteDuration * 20L); // 30 seconds duration
    }

    private void processVoteResults() {
        long yesVotes = votes.values().stream().filter(v -> v).count();
        long totalVotes = votes.size();

        if (totalVotes == 0 || ((double) yesVotes / totalVotes) < requiredVotePercentage) {
            Bukkit.broadcastMessage(ChatColor.RED + "The vote did not gather enough support. The difficulty will remain unchanged.");
        } else {
            Bukkit.broadcastMessage(ChatColor.GREEN + "The vote was successful! Difficulty changed to " + ongoingVoteDifficulty.toUpperCase() + ".");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "difficulty " + ongoingVoteDifficulty);
        }

        ongoingVoteDifficulty = null;
        votes.clear();
    }

    private void cancelVote() {
        ongoingVoteDifficulty = null;
        votes.clear();
    }

    public void handleVote(Player player, boolean vote) {
        if (!votes.containsKey(player)) {
            votes.put(player, vote);
            Bukkit.broadcastMessage(ChatColor.YELLOW + player.getName() + " voted " + (vote ? "Yes" : "No") + ".");
        } else {
            player.sendMessage(ChatColor.RED + "You have already voted.");
        }
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.asList("easy", "normal", "hard", "vote", "forcestop");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("vote")) {
            return java.util.Arrays.asList("yes", "no");
        }
        return null;
    }
}
