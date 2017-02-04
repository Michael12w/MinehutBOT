package com.minehut.discordbot.events;

import com.minehut.discordbot.Core;
import com.minehut.discordbot.commands.Command;
import com.minehut.discordbot.commands.CommandType;
import com.minehut.discordbot.util.Bot;
import com.minehut.discordbot.util.Chat;
import sx.blah.discord.api.IShard;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageUpdateEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by MatrixTunnel on 11/29/2016.
 * Some code provided by the FlareBot developers
 */
public class ChatEvents {

    @EventSubscriber
    public void handle(MessageReceivedEvent event) throws IOException, RateLimitException, DiscordException {
        IMessage message = event.getMessage();
        IShard shard = message.getShard();
        IGuild guild = message.getGuild();
        IUser sender = message.getAuthor();
        IChannel channel = message.getChannel();

        if (guild == null || !shard.isLoggedIn() || !shard.isReady()) {
            return;
        }

        if (channel instanceof IPrivateChannel) {
            Chat.sendMessage("Lol... I don't do things", channel);
            return;
        }

        if (guild == Bot.getMainGuild()) {
            if (sender.getName().equals(sender.getDisplayName(message.getGuild()))) {
                Core.log.info(Chat.getChannelName(channel) + sender.getDisplayName(message.getGuild()) + ": " +
                        Chat.fixDiscordMentions(event.getMessage()));
            } else {
                Core.log.info(Chat.getChannelName(channel) + sender.getDisplayName(message.getGuild()) + " (" +
                        sender.getName() + "): " + Chat.fixDiscordMentions(message));
            }

            if (sender.getID().equals("258699795135201290") || sender.getID().equals("255103056235069440")) return; //TODO Add block command

            if (message.toString().toLowerCase().contains("discord.gg") && !Bot.isTrusted(sender)) {
                Chat.removeMessage(message);

                Chat.sendMessage(sender.mention() + ", please do not advertise Discord servers. Thanks!", channel);
                return;
            }

            if ((message.mentionsEveryone() || message.mentionsHere()) && !Bot.isTrusted(sender)) {
                Chat.sendMessage(sender.mention() + ", you know that mentioning everyone like that is disabled right? xD", channel);
            }

            /*
            //TODO Warn against mentioning staff
            if (channel.getID().equals("239599059415859200") && !Bot.isTrusted(sender)) {
                if (message.getRoleMentions().size() != 0 && (!message.mentionsEveryone() || !message.mentionsHere())) { //TODO Remove @everyone & @here

                    Chat.sendMessage(sender.mention() + ", please do not mention staff in this channel as if gets checked regularly. If it's urgent, please use " +
                            event.getMessage().getGuild().getChannelByID("239599059415859200").mention() + " Thanks! ^-^", channel);
                }
            }
            */
        }

        if (message.getContent() != null && message.getContent().startsWith(Command.getPrefix()) && !sender.isBot()) {

            String msg = message.getContent();
            String command = msg.substring(1);
            String[] args = new String[0];
            if (msg.contains(" ")) {
                command = command.substring(0, msg.indexOf(" ") - 1);
                args = msg.substring(msg.indexOf(" ") + 1).split(" ");
            }

            for (Command cmd : Core.getCommands()) {
                if (cmd.getCommand().equalsIgnoreCase(command)) {
                    if (cmd.getType() == CommandType.ADMINISTRATIVE && !Bot.isTrusted(sender)) {
                        return;
                    }

                    if (cmd.getType() == CommandType.MUSIC && !Bot.getMusicTextChannels().contains(channel.getID())) {
                        return; //TODO Use the #music channel (add "add and remove" music channels)
                    }

                    try {
                        cmd.onCommand(shard, guild, channel, sender, message, args);
                    } catch (Exception ex) {
                        Core.log.error("Exception in guild " + "!\n" + '\'' + cmd.getCommand() + "' "
                                + Arrays.toString(args) + " in " + channel + "! Sender: " +
                                sender.getName() + '#' + sender.getDiscriminator(), ex);
                        ex.printStackTrace();
                    }
                    return;
                } else {
                    for (String alias : cmd.getAliases()) {
                        if (alias.equalsIgnoreCase(command)) {
                            if (cmd.getType() == CommandType.ADMINISTRATIVE && !Bot.isTrusted(sender)) {
                                return;
                            }

                            if (cmd.getType() == CommandType.MUSIC && !Bot.getMusicTextChannels().contains(channel.getID())) {
                                return; //TODO Use the #music channel (add "add and remove" music channels command)
                            }

                            try {
                                cmd.onCommand(shard, guild, channel, sender, message, args);
                            } catch (Exception ex) {
                                Core.log.error("Exception in guild " + "!\n" + '\'' + cmd.getCommand() + "' "
                                        + Arrays.toString(args) + " in " + channel + "! Sender: " +
                                        sender.getName() + '#' + sender.getDiscriminator(), ex);
                                ex.printStackTrace();
                            }
                            return;
                        } else {
                            //not a valid command
                            Chat.setAutoDelete(message, 120);
                        }
                    }
                }
            }
        }
    }

    @EventSubscriber
    public void handle(MessageUpdateEvent event) {
        if (event.getOldMessage().getGuild() != Bot.getMainGuild()) return;
        IMessage oldMessage = event.getOldMessage();
        IMessage newMessage = event.getNewMessage();
        IUser sender = oldMessage.getAuthor();
        IChannel channel = oldMessage.getChannel();

        if (!sender.equals(Core.getDiscord().getOurUser())) {
            if (newMessage.getContent() == null || newMessage.getContent().equals("")) {
                return;
            }

            if (newMessage.getContent().startsWith(Command.getPrefix())) return;
            if (!Chat.logRemove) return;

            if (sender.getName().equals(sender.getDisplayName(oldMessage.getGuild()))) {
                Core.log.info(Chat.getChannelName(channel) + sender.getDisplayName(oldMessage.getGuild()) + " updated message \"" +
                        Chat.fixDiscordMentions(oldMessage) + "\" -> \"" + Chat.fixDiscordMentions(newMessage) + "\"");
            } else {
                Core.log.info(Chat.getChannelName(channel) + sender.getDisplayName(oldMessage.getGuild()) + " (" +
                        sender.getName() + ") updated message \"" +
                        Chat.fixDiscordMentions(oldMessage) + "\" -> \"" + Chat.fixDiscordMentions(newMessage) + "\"");
            }
        }
    }

    @EventSubscriber
    public void handle(MessageDeleteEvent event) {
        if (event.getMessage().getGuild() != Bot.getMainGuild()) return;
        IMessage message = event.getMessage();
        IUser sender = message.getAuthor();
        IChannel channel = message.getChannel();

        if (!sender.equals(Core.getDiscord().getOurUser())) {
            if (message.getContent() == null || message.getContent().equals("")) {
                return;
            }

            if (message.getContent().startsWith(Command.getPrefix())) return;
            if (!Chat.logRemove) return;

            if (sender.getName().equals(sender.getDisplayName(message.getGuild()))) {
                Core.log.info(Chat.getChannelName(channel) + sender.getDisplayName(message.getGuild()) + " removed message \"" + Chat.fixDiscordMentions(message) + "\"");
            } else {
                Core.log.info(Chat.getChannelName(channel) + sender.getDisplayName(message.getGuild()) + " (" +
                        sender.getName() + ") removed message \"" + Chat.fixDiscordMentions(message) + "\"");
            }
        }
    }

}
