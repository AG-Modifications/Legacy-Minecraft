package wily.legacy.mixin;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy.inventory.LegacyMerchantOffer;

import java.util.function.Function;

@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin implements LegacyMerchantOffer {

    @Shadow public abstract ItemStack getResult();

    private int requiredLevel;
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;create(Ljava/util/function/Function;)Lcom/mojang/serialization/Codec;"),remap = false)
    private static Codec<MerchantOffer> init(Function<RecordCodecBuilder.Instance<MerchantOffer>, ? extends App<RecordCodecBuilder.Mu<MerchantOffer>,MerchantOffer>> builder){
        return RecordCodecBuilder.create(instance -> instance.group(ItemCost.CODEC.fieldOf("buy").forGetter(merchantOffer -> merchantOffer.getItemCostA()), ItemCost.CODEC.lenientOptionalFieldOf("buyB").forGetter(MerchantOffer::getItemCostB), ItemStack.CODEC.fieldOf("sell").forGetter(MerchantOffer::getResult), Codec.INT.lenientOptionalFieldOf("uses", 0).forGetter(MerchantOffer::getUses), Codec.INT.lenientOptionalFieldOf("maxUses", 4).forGetter(MerchantOffer::getMaxUses), Codec.BOOL.lenientOptionalFieldOf("rewardExp", true).forGetter(MerchantOffer::shouldRewardExp), Codec.INT.lenientOptionalFieldOf("specialPrice", 0).forGetter(MerchantOffer::getSpecialPriceDiff), Codec.INT.lenientOptionalFieldOf("demand", 0).forGetter(MerchantOffer::getDemand), Codec.FLOAT.lenientOptionalFieldOf("priceMultiplier", Float.valueOf(0.0f)).forGetter(merchantOffer -> Float.valueOf(merchantOffer.getPriceMultiplier())), Codec.INT.lenientOptionalFieldOf("xp", 1).forGetter(MerchantOffer::getXp),Codec.INT.lenientOptionalFieldOf("requiredLevel", 0).forGetter(o->((LegacyMerchantOffer)o).getRequiredLevel())).apply(instance, (a, b, c, d, e, f, g, h, i, j, k)-> {
            MerchantOffer offer = new MerchantOffer(a,b,c,d,e,f,g,h,i,j);
            ((LegacyMerchantOffer)offer).setRequiredLevel(j);
            return offer;
        }));
    }
    @Inject(method = "createFromStream", at = @At("RETURN"))
    private static void createFromStream(RegistryFriendlyByteBuf registryFriendlyByteBuf, CallbackInfoReturnable<MerchantOffer> cir){
        MerchantOffer offer = cir.getReturnValue();
        int level = 0;
        CustomData data = offer.getResult().get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag copy = data.copyTag();
            if (copy.contains("requiredLevel")) {
                level = copy.getInt("requiredLevel");
                copy.remove("requiredLevel");
                if (copy.isEmpty()) offer.getResult().set(DataComponents.CUSTOM_DATA,null);
            }
        }
        ((LegacyMerchantOffer)offer).setRequiredLevel(level);
    }
    @Redirect(method = "writeToStream", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/codec/StreamCodec;encode(Ljava/lang/Object;Ljava/lang/Object;)V", ordinal = 1))
    private static void writeFromStream(StreamCodec<RegistryFriendlyByteBuf,ItemStack> instance, Object b, Object i,RegistryFriendlyByteBuf buf, MerchantOffer offer){
        ItemStack result = offer.getResult().copy();
        CompoundTag customData = result.has(DataComponents.CUSTOM_DATA) ? result.get(DataComponents.CUSTOM_DATA).copyTag() : new CompoundTag();
        customData.putInt("requiredLevel",((LegacyMerchantOffer)offer).getRequiredLevel());
        result.set(DataComponents.CUSTOM_DATA,CustomData.of(customData));
        instance.encode(buf, result);
    }
    @Inject(method = "copy", at = @At("RETURN"))
    private void copy(CallbackInfoReturnable<MerchantOffer> cir){
       ((LegacyMerchantOffer) cir.getReturnValue()).setRequiredLevel(requiredLevel);
    }
    @Override
    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    @Override
    public int getRequiredLevel() {
        return requiredLevel;
    }
}
