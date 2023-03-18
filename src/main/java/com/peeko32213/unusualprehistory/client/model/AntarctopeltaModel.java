package com.peeko32213.unusualprehistory.client.model;


import com.peeko32213.unusualprehistory.UnusualPrehistory;
import com.peeko32213.unusualprehistory.common.entity.EntityAntarctopelta;
import com.peeko32213.unusualprehistory.common.entity.EntityAustroraptor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.processor.IBone;
import software.bernie.geckolib3.model.AnimatedGeoModel;
import software.bernie.geckolib3.model.provider.data.EntityModelData;

import javax.annotation.Nullable;
import java.util.List;

public class AntarctopeltaModel extends AnimatedGeoModel<EntityAntarctopelta>
{
    @Override
    public ResourceLocation getModelResource(EntityAntarctopelta object)
    {
        return new ResourceLocation(UnusualPrehistory.MODID, "geo/antarctopelta.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(EntityAntarctopelta object)
    {
        return new ResourceLocation(UnusualPrehistory.MODID, "textures/entity/antarctopelta.png");
    }

    @Override
    public ResourceLocation getAnimationResource(EntityAntarctopelta object)
    {
        return new ResourceLocation(UnusualPrehistory.MODID, "animations/antarctopelta.animation.json");
    }

    @Override
    public void setLivingAnimations(EntityAntarctopelta dino, Integer uniqueID, @Nullable AnimationEvent customPredicate) {
        super.setLivingAnimations(dino, uniqueID, customPredicate);

        if (customPredicate == null) return;

        List<EntityModelData> extraDataOfType = customPredicate.getExtraDataOfType(EntityModelData.class);
        IBone head = this.getAnimationProcessor().getBone("Head");

        if (dino.isBaby()) {
            head.setScaleX(1.75F);
            head.setScaleY(1.75F);
            head.setScaleZ(1.75F);
        }

        if (!dino.isSprinting()) {
            head.setRotationY(extraDataOfType.get(0).netHeadYaw * Mth.DEG_TO_RAD);
        }
    }

}

