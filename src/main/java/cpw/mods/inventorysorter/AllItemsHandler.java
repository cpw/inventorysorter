package cpw.mods.inventorysorter;

import com.google.common.base.Function;
import net.minecraft.inventory.Slot;

import javax.annotation.Nullable;

/**
 * Created by cpw on 08/01/16.
 */
public enum AllItemsHandler implements Function<Action.ActionContext, Void>
{
    INSTANCE;
    @Nullable
    @Override
    public Void apply(@Nullable Action.ActionContext input)
    {
        return null;
    }
}
