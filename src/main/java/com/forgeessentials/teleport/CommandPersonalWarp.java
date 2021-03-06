package com.forgeessentials.teleport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.permissions.PermissionsManager;
import net.minecraftforge.permissions.PermissionsManager.RegisteredPermValue;

import org.apache.commons.lang3.StringUtils;

import com.forgeessentials.api.APIRegistry;
import com.forgeessentials.core.commands.ForgeEssentialsCommandBase;
import com.forgeessentials.teleport.util.PWarp;
import com.forgeessentials.teleport.util.TeleportDataManager;
import com.forgeessentials.util.OutputHandler;
import com.forgeessentials.util.PlayerInfo;
import com.forgeessentials.util.UserIdent;
import com.forgeessentials.util.selections.WarpPoint;
import com.forgeessentials.util.teleport.TeleportCenter;

public class CommandPersonalWarp extends ForgeEssentialsCommandBase {
	public final String PERM_SET_LIMIT = getPermissionNode() + ".setLimit";
	public final String PERM_LIMIT_PROP = getPermissionNode() + ".max";

	@Override
	public String getCommandName()
	{
		return "personalwarp";
	}

	@Override
	public List<String> getCommandAliases()
	{
		List<String> aliases = new ArrayList<String>();
		aliases.add("pw");
		aliases.add("pwarp");
		return aliases;
	}

	@Override
	public void processCommandPlayer(EntityPlayer sender, String[] args)
	{
		HashMap<String, PWarp> map = TeleportDataManager.pwMap.get(sender.getPersistentID().toString());

		if (map == null)
		{
			map = new HashMap<String, PWarp>();
			TeleportDataManager.pwMap.put(sender.getPersistentID().toString(), map);
		}

		if (args.length == 0)
		{
			if(map.size() == 0)
				OutputHandler.chatNotification(sender, "You have no personal warps.");
			else
			{
				OutputHandler.chatNotification(sender, "Your personal warps:");
				OutputHandler.chatNotification(sender, StringUtils.join(map.keySet().toArray(), ", "));
			}
		}
		else
		{
			if (args[0].equalsIgnoreCase("goto"))
			{
				if (map.containsKey(args[1]))
				{
					PWarp warp = map.get(args[1]);
					PlayerInfo playerInfo = PlayerInfo.getPlayerInfo(sender.getPersistentID());
					playerInfo.setLastTeleportOrigin(new WarpPoint(sender));
					CommandBack.justDied.remove(sender.getPersistentID());
					TeleportCenter.teleport(warp.getPoint(), (EntityPlayerMP) sender);
				}
				else
				{
					OutputHandler.chatError(sender, "That personal warp doesn't exist!");
				}
			}
			else if (args[0].equalsIgnoreCase("add"))
			{
				if (args.length == 1)
                {
                    OutputHandler.chatError(sender, "You must specify a warp name!");
                    return;
                }

                if (!map.containsKey(args[1]))
				{
					Integer prop = APIRegistry.perms.getPermissionPropertyInt(new UserIdent(sender), PERM_LIMIT_PROP);
					if (prop == null || prop == -1)
					{
						map.put(args[1], new PWarp(sender.getPersistentID().toString(), args[1], new WarpPoint(sender)));
						OutputHandler.chatConfirmation(sender, "Personal warp sucessfully added.");
					}
					else if (map.size() < prop)
					{
						map.put(args[1], new PWarp(sender.getPersistentID().toString(), args[1], new WarpPoint(sender)));
						OutputHandler.chatConfirmation(sender, "Personal warp sucessfully added.");
					}
					else
					{
						OutputHandler.chatError(sender, "You have reached your limit.");
					}
				}
				else
				{
					OutputHandler.chatError(sender, "That personal warp already exists.");
				}
			}
			else if (args[0].equalsIgnoreCase("remove"))
			{
                if (args[1] == null)
                {
                    OutputHandler.chatError(sender, "You must specify a warp name!");
                    return;
                }

                if (map.containsKey(args[1]))
				{
					TeleportDataManager.removePWarp(map.get(args[1]));
					map.remove(args[1]);
					OutputHandler.chatConfirmation(sender, "Personal warp sucessfully removed.");
				}
				else
				{
					OutputHandler.chatError(sender, "That personal warp doesn't exist!");
				}
			}
			else if (args[0].equalsIgnoreCase("limit") && PermissionsManager.checkPermission(sender, PERM_SET_LIMIT))
			{
				if (args.length == 1)
				{
					OutputHandler.chatError(sender, "Specify a group or player. (-1 means no limit.)");
				}
				else
				{
					String target;
					if (APIRegistry.perms.groupExists(args[1]))
					{
						target = "g:" + args[1];
					}
					else if (args[1].equalsIgnoreCase("me"))
					{
						target = "p:" + sender.getCommandSenderName();
					}
					else
					{
						target = "p:" + UserIdent.getPlayerByMatchOrUsername(sender, args[1]).getCommandSenderName();
					}

					if (args.length == 2)
					{
						OutputHandler.chatConfirmation(sender, String.format("The current limit is %s.", getLimit(target)));
					}
					else
					{
						setLimit(target, parseIntWithMin(sender, args[2], -1));
						OutputHandler.chatConfirmation(sender, String.format("Limit changed to %s.", getLimit(target)));
					}

				}
			}
			else if (args[0].equalsIgnoreCase("limit"))
			{
				OutputHandler.chatConfirmation(sender, String.format("The current limit is %s.", getLimit(sender)));
			}
		}
		TeleportDataManager.pwMap.put(sender.getPersistentID().toString(), map);
		TeleportDataManager.savePWarps(sender.getPersistentID().toString());
	}

	private String getLimit(EntityPlayer sender)
	{
		return APIRegistry.perms.getPermissionProperty(sender, PERM_LIMIT_PROP);
	}

	private String getLimit(String target)
	{
		if (target.startsWith("p:"))
		{
			return APIRegistry.perms.getPermissionProperty(new UserIdent(target.replaceFirst("p:", "")), PERM_LIMIT_PROP);
		}
		else if (target.startsWith("g:"))
		{
			return APIRegistry.perms.getPermissionProperty(new UserIdent(target.replaceFirst("g:", "")), PERM_LIMIT_PROP);
		}
		else
		{
			return "";
		}
	}

	private void setLimit(String target, int limit)
	{
		if (target.startsWith("p:"))
		{
			APIRegistry.perms.setPlayerPermissionProperty(new UserIdent(target.replaceFirst("p:", "")), PERM_LIMIT_PROP, Integer.toString(limit));
		}
		else if (target.startsWith("g:"))
		{
			APIRegistry.perms.setGroupPermissionProperty(target.replaceFirst("g:", ""), PERM_LIMIT_PROP, Integer.toString(limit));
		}
		else
		{
			return;
		}
	}

	@Override
	public boolean canConsoleUseCommand()
	{
		return false;
	}

	@Override
	public String getPermissionNode()
	{
		return "fe.teleport." + getCommandName();
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args)
	{
		if (args.length == 1)
		{
			return getListOfStringsMatchingLastWord(args, "goto", "add", "remove", "limit");
		}
		// TODO: Not yet implemented!
//		if (args.length == 2 && args[0].equalsIgnoreCase("limit"))
//		{
//			Zone zone = sender instanceof EntityPlayer ? APIRegistry.perms.getZoneAt(new WorldPoint((EntityPlayer) sender)) : APIRegistry.perms.getGLOBAL();
//			ArrayList<String> list = new ArrayList<String>();
//			for (String s : FMLCommonHandler.instance().getMinecraftServerInstance().getAllUsernames())
//			{
//				list.add(s);
//			}
//
//			while (zone != null)
//			{
//				for (Group g : APIRegistry.perms.getGroupsInZone(zone.getName()))
//				{
//					list.add(g.name);
//				}
//				zone = APIRegistry.perms.getZone(zone.parent);
//			}
//
//			return getListOfStringsFromIterableMatchingLastWord(args, list);
//		}
		if (args.length == 2)
		{
			if (TeleportDataManager.pwMap.get(sender.getCommandSenderName()) == null)
			{
				TeleportDataManager.pwMap.put(sender.getCommandSenderName(), new HashMap<String, PWarp>());
			}
			return getListOfStringsFromIterableMatchingLastWord(args, TeleportDataManager.pwMap.get(sender.getCommandSenderName()).keySet());
		}
		return null;
	}

	@Override
	public RegisteredPermValue getDefaultPermission()
	{
		return RegisteredPermValue.TRUE;
	}

	public void registerExtraPermissions()
	{
	    APIRegistry.perms.registerPermission(PERM_SET_LIMIT, RegisteredPermValue.OP, "Allow setting the warp limit for players");
		APIRegistry.perms.registerPermissionProperty(PERM_LIMIT_PROP, "10", "Maximum number of personal warps a player can create");
		// APIRegistry.perms.registerPermissionProperty(PERMPROP, 0, GUEST);
		// APIRegistry.perms.registerPermissionProperty(PERMPROP, 10, MEMBER);
		// APIRegistry.perms.registerPermissionProperty(PERMPROP, -1, OP);
	}

	@Override
	public String getCommandUsage(ICommandSender sender)
	{
		return "/pwarp goto [name] OR <add|remove> <name> Teleports you to a personal warp.";
	}
}
