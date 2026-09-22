package com.example.pickuptweaks.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@code ItemEntity#pickupDelay} は private のため、Accessor 経由で読む。
 *
 * <p>バニラに {@code hasPickUpDelay()} が存在する場合はそちらでも代用できるが、
 * Accessor のほうがバージョン間で壊れにくい。</p>
 */
@Mixin(ItemEntity.class)
public interface ItemEntityAccessor {

	@Accessor("pickupDelay")
	int getPickupDelay();
}
