package com.forgeessentials.util.events;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

import com.forgeessentials.util.selections.WarpPoint;

import cpw.mods.fml.common.eventhandler.Cancelable;

@Cancelable
public class PlayerMoveEvent extends PlayerEvent {
    public final WarpPoint before;
    public final WarpPoint after;

    public PlayerMoveEvent(EntityPlayer player, WarpPoint before, WarpPoint after)
    {
        super(player);
        this.before = before;
        this.after = after;
    }

    public boolean isViewMove()
    {
        return before.yaw != after.yaw && before.pitch != after.pitch;
    }

    public boolean isCoordMove()
    {
        return before.xd != after.xd && before.yd != after.yd && before.zd != after.zd;
    }

    public boolean isBlockMove()
    {
        return before.getX() != after.getX() && before.getY() != after.getY() && before.getZ() != after.getZ();
    }

}
