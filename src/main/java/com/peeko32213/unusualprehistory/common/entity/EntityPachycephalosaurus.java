package com.peeko32213.unusualprehistory.common.entity;

import com.peeko32213.unusualprehistory.UnusualPrehistory;
import com.peeko32213.unusualprehistory.common.entity.util.BabyPanicGoal;
import com.peeko32213.unusualprehistory.common.entity.util.HitboxHelper;
import com.peeko32213.unusualprehistory.common.entity.util.LandCreaturePathNavigation;
import com.peeko32213.unusualprehistory.core.registry.UPEntities;
import com.peeko32213.unusualprehistory.core.registry.UPItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib3.core.AnimationState;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityPachycephalosaurus extends Animal implements IAnimatable {
    private static final TagKey<Item> FOOD = ForgeRegistries.ITEMS.tags().createTagKey(new ResourceLocation(UnusualPrehistory.MODID, "pachy_food"));
    private static final EntityDataAccessor<Integer> ANIMATION_STATE = SynchedEntityData.defineId(EntityPachycephalosaurus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COMBAT_STATE = SynchedEntityData.defineId(EntityPachycephalosaurus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ENTITY_STATE = SynchedEntityData.defineId(EntityPachycephalosaurus.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PASSIVE = SynchedEntityData.defineId(EntityPachycephalosaurus.class, EntityDataSerializers.INT);

    private final AnimationFactory factory = new AnimationFactory(this);

    public EntityPachycephalosaurus(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
        this.maxUpStep = 1.0f;
    }


    @Override
    protected PathNavigation createNavigation(Level level) {
        return new LandCreaturePathNavigation(this, level);
    }


    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 13.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5D)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0D);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new EntityPachycephalosaurus.PachyMeleeAttackGoal(this,  1.3F, true));
        this.goalSelector.addGoal(3, new BabyPanicGoal(this, 2.0D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.0D, Ingredient.of(Items.WHEAT), false));
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(this, Player.class, 100, true, false, this::isAngryAt));
        this.targetSelector.addGoal(8, (new HurtByTargetGoal(this)));
    }

    public boolean isAngryAt(LivingEntity p_21675_) {
        return this.canAttack(p_21675_);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(PASSIVE, 0);
        this.entityData.define(ANIMATION_STATE, 0);
        this.entityData.define(COMBAT_STATE, 0);
        this.entityData.define(ENTITY_STATE, 0);

    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("PassiveTicks", this.getPassiveTicks());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.setPassiveTicks(compound.getInt("PassiveTicks"));

    }

    public int getPassiveTicks() {
        return this.entityData.get(PASSIVE);
    }

    private void setPassiveTicks(int passiveTicks) {
        this.entityData.set(PASSIVE, passiveTicks);
    }

    public int getAnimationState() {

        return this.entityData.get(ANIMATION_STATE);
    }

    public void setAnimationState(int anim) {

        this.entityData.set(ANIMATION_STATE, anim);
    }

    public int getCombatState() {

        return this.entityData.get(COMBAT_STATE);
    }

    public void setCombatState(int anim) {

        this.entityData.set(COMBAT_STATE, anim);
    }

    public int getEntityState() {

        return this.entityData.get(ENTITY_STATE);
    }

    public void setEntityState(int anim) {

        this.entityData.set(ENTITY_STATE, anim);
    }

    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (isFood(itemstack) && (this.getPassiveTicks() <= 0 || this.getHealth() < this.getMaxHealth())) {
            if (!player.isCreative()) {
                itemstack.shrink(1);
            }
            this.heal(2);
            this.setPassiveTicks(this.getPassiveTicks() + 1500);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12000));
            return InteractionResult.SUCCESS;
        }
        InteractionResult type = super.mobInteract(player, hand);
        return type;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel serverLevel, AgeableMob ageableMob) {
        return UPEntities.PACHY.get().create(serverLevel);
    }

    public void tick() {
        super.tick();
        if (this.getTarget() != null && this.getTarget().hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
            this.setTarget(null);
            this.setLastHurtByMob(null);
        }

    }

    public boolean hurt(DamageSource source, float amount) {
        boolean prev = super.hurt(source, amount);
        if (prev) {
            if (source.getEntity() != null) {
                if (source.getEntity() instanceof LivingEntity) {
                    LivingEntity hurter = (LivingEntity) source.getEntity();
                    if (hurter.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
                        hurter.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                    }
                }
            }
            return prev;
        }
        return prev;
    }


    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        int animState = this.getAnimationState();
        {
            switch (animState) {

                case 21:
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.pachy.headswing1", false));
                    break;
                case 22:
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.pachy.headswing2", false));
                    break;
                case 23:
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.pachy.attack", false));
                    break;
                case 24:
                    event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.pachy.kick", false));
                    break;
                default:
                    if (!(event.getLimbSwingAmount() > -0.06F && event.getLimbSwingAmount() < 0.06F)) {
                        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.pachy.walk", true));

                    } else {
                        event.getController().setAnimation(new AnimationBuilder().addAnimation("animation.pachy.idle", true));
                        event.getController().setAnimationSpeed(1.0F);

                    }
                    break;

            }
        }
        return PlayState.CONTINUE;
    }


    @Override
    public void registerControllers(AnimationData data) {
        data.setResetSpeedInTicks(5);
        AnimationController<EntityPachycephalosaurus> controller = new AnimationController<>(this, "controller", 5, this::predicate);
        data.addAnimationController(controller);
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    static class PachyMeleeAttackGoal extends Goal {

        protected final EntityPachycephalosaurus mob;
        private final double speedModifier;
        private final boolean followingTargetEvenIfNotSeen;
        private Path path;
        private double pathedTargetX;
        private double pathedTargetY;
        private double pathedTargetZ;
        private int ticksUntilNextPathRecalculation;
        private int ticksUntilNextAttack;
        private long lastCanUseCheck;
        private int failedPathFindingPenalty = 0;
        private boolean canPenalize = false;
        private int animTime = 0;


        public PachyMeleeAttackGoal(EntityPachycephalosaurus p_i1636_1_, double p_i1636_2_, boolean p_i1636_4_) {
            this.mob = p_i1636_1_;
            this.speedModifier = p_i1636_2_;
            this.followingTargetEvenIfNotSeen = p_i1636_4_;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        public boolean canUse() {
            long i = this.mob.level.getGameTime();

            if (i - this.lastCanUseCheck < 20L) {
                return false;
            } else {
                this.lastCanUseCheck = i;
                LivingEntity livingentity = this.mob.getTarget();
                if (livingentity == null) {
                    return false;
                } else if (!livingentity.isAlive()) {
                    return false;
                } else {
                    if (canPenalize) {
                        if (--this.ticksUntilNextPathRecalculation <= 0) {
                            this.path = this.mob.getNavigation().createPath(livingentity, 0);
                            this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
                            return this.path != null;
                        } else {
                            return true;
                        }
                    }
                    this.path = this.mob.getNavigation().createPath(livingentity, 0);
                    if (this.path != null) {
                        return true;
                    } else {
                        return this.getAttackReachSqr(livingentity) >= this.mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
                    }
                }
            }

        }

        public boolean canContinueToUse() {

            LivingEntity livingentity = this.mob.getTarget();

            if (livingentity == null) {
                return false;
            } else if (!livingentity.isAlive()) {
                return false;
            } else if (!this.followingTargetEvenIfNotSeen) {
                return !this.mob.getNavigation().isDone();
            } else if (!this.mob.isWithinRestriction(livingentity.blockPosition())) {
                return false;
            } else {
                return !(livingentity instanceof Player) || !livingentity.isSpectator() && !((Player) livingentity).isCreative();
            }

        }

        public void start() {
            this.mob.getNavigation().moveTo(this.path, this.speedModifier);
            this.mob.setAggressive(true);
            this.ticksUntilNextPathRecalculation = 0;
            this.ticksUntilNextAttack = 0;
            this.animTime = 0;
            this.mob.setAnimationState(0);

        }

        public void stop() {
            LivingEntity livingentity = this.mob.getTarget();
            if (!EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(livingentity)) {
                this.mob.setTarget((LivingEntity) null);
            }
            this.mob.setAnimationState(0);
            this.mob.setAggressive(false);
        }

        public void tick() {


            LivingEntity target = this.mob.getTarget();
            double distance = this.mob.distanceToSqr(target.getX(), target.getY(), target.getZ());
            double reach = this.getAttackReachSqr(target);
            int animState = this.mob.getAnimationState();
            Vec3 aim = this.mob.getLookAngle();
            Vec2 aim2d = new Vec2((float) (aim.x / (1 - Math.abs(aim.y))), (float) (aim.z / (1 - Math.abs(aim.y))));


            switch (animState) {
                case 21:
                    tickLightAttack1();
                    break;
                case 22:
                    tickLightAttack2();
                    break;
                case 23:
                    tickStrongHeadbuttAttack();
                    break;
                case 24:
                    tickKickAttack();
                    break;
                default:
                    this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
                    this.ticksUntilNextAttack = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);
                    this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
                    this.doMovement(target, distance);
                    this.checkForCloseRangeAttack(distance, reach);
                    break;

            }

        }

            protected void doMovement (LivingEntity livingentity, Double d0){


                this.ticksUntilNextPathRecalculation = Math.max(this.ticksUntilNextPathRecalculation - 1, 0);


                if ((this.followingTargetEvenIfNotSeen || this.mob.getSensing().hasLineOfSight(livingentity)) && this.ticksUntilNextPathRecalculation <= 0 && (this.pathedTargetX == 0.0D && this.pathedTargetY == 0.0D && this.pathedTargetZ == 0.0D || livingentity.distanceToSqr(this.pathedTargetX, this.pathedTargetY, this.pathedTargetZ) >= 1.0D || this.mob.getRandom().nextFloat() < 0.05F)) {
                    this.pathedTargetX = livingentity.getX();
                    this.pathedTargetY = livingentity.getY();
                    this.pathedTargetZ = livingentity.getZ();
                    this.ticksUntilNextPathRecalculation = 4 + this.mob.getRandom().nextInt(7);
                    if (this.canPenalize) {
                        this.ticksUntilNextPathRecalculation += failedPathFindingPenalty;
                        if (this.mob.getNavigation().getPath() != null) {
                            Node finalPathPoint = this.mob.getNavigation().getPath().getEndNode();
                            if (finalPathPoint != null && livingentity.distanceToSqr(finalPathPoint.x, finalPathPoint.y, finalPathPoint.z) < 1)
                                failedPathFindingPenalty = 0;
                            else
                                failedPathFindingPenalty += 10;
                        } else {
                            failedPathFindingPenalty += 10;
                        }
                    }
                    if (d0 > 1024.0D) {
                        this.ticksUntilNextPathRecalculation += 10;
                    } else if (d0 > 256.0D) {
                        this.ticksUntilNextPathRecalculation += 5;
                    }

                    if (!this.mob.getNavigation().moveTo(livingentity, this.speedModifier)) {
                        this.ticksUntilNextPathRecalculation += 15;
                    }
                }

            }


            protected void checkForCloseRangeAttack ( double distance, double reach){
                if (distance <= reach && this.ticksUntilNextAttack <= 0) {


                    int r = this.mob.getRandom().nextInt(2048);
                    if (r <= 800) {
                        this.mob.setAnimationState(21);
                    } else if (r <= 1300) {
                        this.mob.setAnimationState(22);
                    } else if (r <= 1850) {
                        this.mob.setAnimationState(23);
                    } else {
                        this.mob.setAnimationState(24);
                    }

                }
            }


            protected boolean getRangeCheck () {

                return
                        this.mob.distanceToSqr(this.mob.getTarget().getX(), this.mob.getTarget().getY(), this.mob.getTarget().getZ())
                                <=
                                1.3F * this.getAttackReachSqr(this.mob.getTarget());

            }



        protected void tickLightAttack1 () {
            animTime++;
            if(animTime==4) {
                performLightAttack();
            }
            if(animTime>=8) {
                animTime=0;
                if (this.getRangeCheck()) {
                    this.mob.setAnimationState(22);
                }else {
                    this.mob.setAnimationState(0);
                    this.resetAttackCooldown();
                    this.ticksUntilNextPathRecalculation = 0;
                }
            }
        }

        protected void tickLightAttack2 () {
            animTime++;

            if(animTime==4) {
                performLightAttack();
            }
            if(animTime>=7) {
                animTime=0;

                this.mob.setAnimationState(0);
                this.resetAttackCooldown();
                this.ticksUntilNextPathRecalculation = 0;

            }

        }

            protected void tickStrongHeadbuttAttack () {
                animTime++;
                if(animTime==8) {
                    performStrongAttack();
                }
                if(animTime>=14) {
                    animTime=0;
                    this.mob.setAnimationState(0);
                    this.resetAttackCooldown();
                    this.ticksUntilNextPathRecalculation = 0;
                }

            }
            protected void tickKickAttack () {
                animTime++;
                if(animTime==7) {
                    performAttackKick();
                }
                if(animTime>=12) {
                    animTime=0;
                    this.mob.setAnimationState(0);
                    this.resetAttackCooldown();
                    this.ticksUntilNextPathRecalculation = 0;
                }

            }


            protected void performLightAttack () {


                Vec3 pos = mob.position();
                this.mob.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0f, 0.2f);
                HitboxHelper.LargeAttackWithTargetCheck(DamageSource.mobAttack(mob),4.0f, 0.1f, mob, pos,  2.1F, -Math.PI/5, Math.PI/3, -1.0f, 3.0f);

            }

        protected void performStrongAttack () {


            Vec3 pos = mob.position();
            this.mob.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0f, 0.2f);
            HitboxHelper.LargeAttackWithTargetCheck(DamageSource.mobAttack(mob),8.0f, 0.1f, mob, pos,  2.1F, -Math.PI/5, Math.PI/3, -1.0f, 3.0f);

        }


        protected void performAttackKick () {


            Vec3 pos = mob.position();
            this.mob.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 2.0f, 0.2f);
            HitboxHelper.LargeAttackWithTargetCheck(DamageSource.mobAttack(mob),10.0f, 1.0f, mob, pos,  2.1F, -Math.PI/5, Math.PI/3, -1.0f, 3.0f);

        }


            protected void resetAttackCooldown () {
                this.ticksUntilNextAttack = 0;
            }

            protected boolean isTimeToAttack () {
                return this.ticksUntilNextAttack <= 0;
            }

            protected int getTicksUntilNextAttack () {
                return this.ticksUntilNextAttack;
            }

            protected int getAttackInterval () {
                return 3;
            }

        protected double getAttackReachSqr(LivingEntity p_25556_) {
            return (double)(this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 1.3F + p_25556_.getBbWidth());
        }
        }


    }
