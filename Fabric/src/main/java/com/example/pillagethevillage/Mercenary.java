// SPDX-License-Identifier: LGPL-3.0-only
package com.example.pillagethevillage;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedCrossbowAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class Mercenary extends AbstractIllager implements OwnableEntity, CrossbowAttackMob {
    private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> OWNER =
            SynchedEntityData.defineId(Mercenary.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);
    private static final EntityDataAccessor<Boolean> TAMED =
            SynchedEntityData.defineId(Mercenary.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FOLLOWING =
            SynchedEntityData.defineId(Mercenary.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CHARGING_CROSSBOW =
            SynchedEntityData.defineId(Mercenary.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> SKIN_VARIANT =
            SynchedEntityData.defineId(Mercenary.class, EntityDataSerializers.STRING);

    private int ownerAttackTimestamp;
    private int ownerHurtTimestamp;
    private int behaviorTick;
    private BlockPos patrolCenter;
    private int volunteerDespawnDelay = -1;

    public Mercenary(EntityType<? extends Mercenary> type, Level level) {
        super(type, level);
        this.getNavigation().setCanOpenDoors(true);
        this.getNavigation().setCanFloat(true);
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.SCALE, 0.95);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(OWNER, Optional.empty());
        data.define(TAMED, false);
        data.define(FOLLOWING, false);
        data.define(CHARGING_CROSSBOW, false);
        data.define(SKIN_VARIANT, "plains");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
            @Override
            public boolean canUse() {
                return Mercenary.this.hasCombatWeapon()
                        && !Mercenary.this.isRangedWeapon()
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                LivingEntity target = Mercenary.this.getTarget();
                return target != null && target.isAlive() && Mercenary.this.canAttack(target)
                        && Mercenary.this.hasCombatWeapon() && !Mercenary.this.isRangedWeapon()
                        && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(3, new RangedBowAttackGoal<Mercenary>(this, 1.0, 20, 15.0F) {
            @Override
            public boolean canUse() {
                return Mercenary.this.hasCombatWeapon()
                        && Mercenary.this.getMainHandItem().getItem() instanceof BowItem
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                LivingEntity target = Mercenary.this.getTarget();
                return target != null && target.isAlive() && Mercenary.this.canAttack(target)
                        && Mercenary.this.hasCombatWeapon()
                        && Mercenary.this.getMainHandItem().getItem() instanceof BowItem
                        && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(3, new RangedCrossbowAttackGoal<Mercenary>(this, 1.0, 15.0F) {
            @Override
            public boolean canUse() {
                return Mercenary.this.hasCombatWeapon()
                        && Mercenary.this.getMainHandItem().getItem() instanceof CrossbowItem
                        && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                LivingEntity target = Mercenary.this.getTarget();
                return target != null && target.isAlive() && Mercenary.this.canAttack(target)
                        && Mercenary.this.hasCombatWeapon()
                        && Mercenary.this.getMainHandItem().getItem() instanceof CrossbowItem
                        && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(5, new OpenWoodenDoorGoal(this));
        this.goalSelector.addGoal(8, new RandomStrollGoal(this, 0.6) {
            @Override
            public boolean canUse() {
                return (!Mercenary.this.isTame() || !Mercenary.this.isFollowingOwner()) && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return (!Mercenary.this.isTame() || !Mercenary.this.isFollowingOwner()) && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<Monster>(this, Monster.class, 10, true, false,
                (target, level) -> this.hasCombatWeapon() && target.isAlive()
                        && !(target instanceof Creeper) && !target.is(EntityTypeTags.ILLAGER)));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean ownerInteraction = this.isTame() && this.getOwner() == player;

        if (player.isCrouching() && isWeapon(held)) {
            if (!ownerInteraction) return InteractionResult.PASS;
            if (!this.level().isClientSide()) {
                ItemStack previous = this.getMainHandItem().copy();
                this.setItemSlot(EquipmentSlot.MAINHAND, held.copyWithCount(1));
                this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
                if (player instanceof ServerPlayer serverPlayer && isNetheriteWeapon(held)) {
                    MercenaryAdvancements.award(serverPlayer, "adventure/netherite_gear", "netherite_weapon_given");
                }
                if (!player.getAbilities().instabuild) held.shrink(1);
                if (!previous.isEmpty()) this.spawnAtLocation((ServerLevel) this.level(), previous);
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.isCrouching() && ownerInteraction && held.isEmpty()) {
            if (!this.level().isClientSide()) this.entityData.set(FOLLOWING, !this.isFollowingOwner());
            return InteractionResult.SUCCESS;
        }
        if (!this.isTame() && held.is(Items.EMERALD)) {
            if (!this.level().isClientSide()) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                this.entityData.set(TAMED, true);
                this.entityData.set(OWNER, Optional.of(EntityReference.of(player)));
                this.entityData.set(FOLLOWING, true);
                this.patrolCenter = this.blockPosition();
                this.volunteerDespawnDelay = -1;
                this.ownerAttackTimestamp = player.getLastHurtMobTimestamp();
                this.ownerHurtTimestamp = player.getLastHurtByMobTimestamp();
                if (player instanceof ServerPlayer serverPlayer) {
                    MercenaryAdvancements.award(serverPlayer, "adventure/hire_a_mercenary", "hired");
                    String variant = this.getSkinVariant();
                    if (isKnownBiomeType(variant)) {
                        MercenaryAdvancements.award(serverPlayer, "adventure/soldiers_without_borders", variant);
                    }
                }
                this.playSound(SoundEvents.PILLAGER_CELEBRATE, 1.0F, 1.0F);
                this.level().broadcastEntityEvent(this, (byte) 7);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public boolean isTame() {
        return this.entityData.get(TAMED);
    }

    public boolean isFollowingOwner() {
        return this.entityData.get(FOLLOWING);
    }

    public String getSkinVariant() {
        return this.entityData.get(SKIN_VARIANT);
    }

    public void setSkinVariant(String variant) {
        this.entityData.set(SKIN_VARIANT, variant);
    }

    public void setVolunteerDespawnDelay(int ticks) {
        this.volunteerDespawnDelay = ticks;
    }
    public boolean isTemporaryVolunteer() {
        return !this.isTame() && "volunteer".equals(this.getSkinVariant()) && this.volunteerDespawnDelay > 0;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 7) {
            for (int i = 0; i < 7; i++) {
                this.level().addParticle(ParticleTypes.HEART, this.getRandomX(1.0), this.getRandomY() + 0.5,
                        this.getRandomZ(1.0), this.random.nextGaussian() * 0.02, this.random.nextGaussian() * 0.02,
                        this.random.nextGaussian() * 0.02);
            }
        } else super.handleEntityEvent(id);
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
        String biomeType = VillagerType.byBiome(level.getBiome(this.blockPosition())).identifier().getPath();
        if (!isKnownBiomeType(biomeType)) biomeType = "plains";
        if (reason == EntitySpawnReason.NATURAL && this.random.nextInt(20) == 0) biomeType = "volunteer";
        this.entityData.set(SKIN_VARIANT, biomeType);
        this.setCanJoinRaid(false);
        return result;
    }

    private static boolean isKnownBiomeType(String variant) {
        return switch (variant) {
            case "desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga" -> true;
            default -> false;
        };
    }

    private boolean hasCombatWeapon() {
        return this.isTame() && isWeapon(this.getMainHandItem());
    }
    private boolean isRangedWeapon() {
        ItemStack weapon = this.getMainHandItem();
        return weapon.getItem() instanceof BowItem || weapon.getItem() instanceof CrossbowItem;
    }

    public static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SWORDS) || stack.is(ItemTags.HOES)
                || stack.is(ItemTags.AXES) || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem;
    }

    private static boolean isNetheriteWeapon(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().startsWith("netherite_")
                && isWeapon(stack);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel && !this.isTame() && this.volunteerDespawnDelay > 0
                && --this.volunteerDespawnDelay == 0) {
            this.discard();
            return;
        }
        if (!(this.level() instanceof ServerLevel) || !this.isTame()) return;

        LivingEntity target = this.getTarget();
        if (target != null && (!target.isAlive() || !this.canAttack(target))) {
            this.setTarget(null);
            this.stopUsingItem();
        }

        if (this.hasCombatWeapon()) {
            LivingEntity owner = this.getOwner();
            if (owner != null && owner.getLastHurtMobTimestamp() != this.ownerAttackTimestamp) {
                this.ownerAttackTimestamp = owner.getLastHurtMobTimestamp();
                LivingEntity attacked = owner.getLastHurtMob();
                if (attacked != null && attacked != this && this.canAttack(attacked)) this.setTarget(attacked);
            }
            if (owner != null && owner.getLastHurtByMobTimestamp() != this.ownerHurtTimestamp) {
                this.ownerHurtTimestamp = owner.getLastHurtByMobTimestamp();
                LivingEntity attacker = owner.getLastHurtByMob();
                if (attacker != null && attacker != this && this.canAttack(attacker)) this.setTarget(attacker);
            }
        }

        if (this.getTarget() != null || ++this.behaviorTick % 10 != 0) return;
        LivingEntity owner = this.getOwner();
        if (this.isFollowingOwner() && owner != null) {
            double distance = this.distanceToSqr(owner);
            if (distance > 9.0) this.getNavigation().moveTo(owner, 1.05);
            if (distance > 144.0) this.teleportNearOwner(owner);
        } else if (this.patrolCenter != null && this.distanceToSqr(this.patrolCenter.getX() + 0.5,
                this.patrolCenter.getY(), this.patrolCenter.getZ() + 0.5) > 100.0) {
            this.getNavigation().moveTo(this.patrolCenter.getX() + 0.5, this.patrolCenter.getY(),
                    this.patrolCenter.getZ() + 0.5, 0.8);
        }
    }

    private void teleportNearOwner(LivingEntity owner) {
        if (!owner.onGround() || owner.isInLiquid()) return;
        BlockPos ownerPos = owner.blockPosition();
        BlockPos ownerSupport = ownerPos.below();
        if (!owner.level().getBlockState(ownerSupport).isFaceSturdy(owner.level(), ownerSupport, Direction.UP)
                || !owner.level().getFluidState(ownerPos).isEmpty()) return;

        for (int radius = 1; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos destination = ownerPos.offset(dx, 0, dz);
                    BlockPos support = destination.below();
                    if (!this.level().getBlockState(support).isFaceSturdy(this.level(), support, Direction.UP)
                            || !this.level().getFluidState(destination).isEmpty()) continue;
                    double x = destination.getX() + 0.5;
                    double y = destination.getY();
                    double z = destination.getZ() + 0.5;
                    if (!this.level().noCollision(this, this.getBoundingBox().move(
                            x - this.getX(), y - this.getY(), z - this.getZ()))) continue;
                    this.teleportTo(x, y, z);
                    return;
                }
            }
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        if (!this.hasCombatWeapon() || target == this.getOwner() || target instanceof Creeper) return false;
        LivingEntity owner = this.getOwner();
        boolean ownerOrderedAttack = owner != null
                && (owner.getLastHurtMob() == target || owner.getLastHurtByMob() == target);
        return ownerOrderedAttack || !this.isAlliedTo(target);
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity target, DamageSource source) {
        boolean result = super.killedEntity(level, target, source);
        if (this.isTame() && target.getType().builtInRegistryHolder().is(EntityTypeTags.ILLAGER)
                && this.getOwner() instanceof ServerPlayer owner) {
            MercenaryAdvancements.award(owner, "adventure/friendly_fire", "illager_killed");
        }
        return result;
    }

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(CHARGING_CROSSBOW, charging);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        ItemStack weapon = this.getMainHandItem();
        if (weapon.getItem() instanceof CrossbowItem) {
            this.performCrossbowAttack(this, 1.6F);
        } else if (weapon.getItem() instanceof BowItem bow && this.level() instanceof ServerLevel serverLevel) {
            ItemStack projectile = this.getProjectile(weapon);
            if (projectile.isEmpty()) return;
            AbstractArrow arrow = ProjectileUtil.getMobArrow(this, projectile, power, weapon);
            double dx = target.getX() - this.getX();
            double dy = target.getY(0.3333333333333333) - arrow.getY();
            double dz = target.getZ() - this.getZ();
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            Projectile.spawnProjectileUsingShoot(arrow, serverLevel, projectile, dx,
                    dy + horizontalDistance * 0.2F, dz, 1.6F, this.rangedAttackUncertainty(serverLevel));
            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F,
                    1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        }
    }

    @Override
    public AbstractIllager.IllagerArmPose getArmPose() {
        if (this.entityData.get(CHARGING_CROSSBOW)) return AbstractIllager.IllagerArmPose.CROSSBOW_CHARGE;
        if (this.getMainHandItem().getItem() instanceof CrossbowItem) return AbstractIllager.IllagerArmPose.CROSSBOW_HOLD;
        if (!this.isAggressive()) return AbstractIllager.IllagerArmPose.CROSSED;
        if (this.getMainHandItem().getItem() instanceof BowItem) return AbstractIllager.IllagerArmPose.BOW_AND_ARROW;
        return AbstractIllager.IllagerArmPose.ATTACKING;
    }

    @Override
    public void applyRaidBuffs(ServerLevel level, int wave, boolean isCaptain) {
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return SoundEvents.PILLAGER_CELEBRATE;
    }

    @Override
    public boolean canBeLeader() {
        return false;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PILLAGER_AMBIENT;
    }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return SoundEvents.PILLAGER_HURT;
    }
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }

    private static final class OpenWoodenDoorGoal extends DoorInteractGoal {
        private float openDirectionX;
        private float openDirectionZ;
        private boolean passed;

        private OpenWoodenDoorGoal(Mercenary mercenary) {
            super(mercenary);
        }

        @Override
        public void start() {
            super.start();
            this.setOpen(true);
            this.passed = false;
            this.openDirectionX = (float)(this.doorPos.getX() + 0.5 - this.mob.getX());
            this.openDirectionZ = (float)(this.doorPos.getZ() + 0.5 - this.mob.getZ());
        }

        @Override
        public boolean canContinueToUse() {
            return !this.passed && super.canContinueToUse();
        }

        @Override
        public void tick() {
            super.tick();
            float dx = (float)(this.doorPos.getX() + 0.5 - this.mob.getX());
            float dz = (float)(this.doorPos.getZ() + 0.5 - this.mob.getZ());
            if (this.openDirectionX * dx + this.openDirectionZ * dz < 0.0F) {
                this.passed = true;
                this.setOpen(false);
            }
        }
    }

    @Override
    public @Nullable EntityReference<LivingEntity> getOwnerReference() {
        return this.entityData.get(OWNER).orElse(null);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        EntityReference.store(this.getOwnerReference(), output, "MercenaryOwner");
        output.putBoolean("MercenaryTamed", this.isTame());
        output.putBoolean("MercenaryFollowing", this.isFollowingOwner());
        output.putString("MercenaryVariant", this.getSkinVariant());
        output.putInt("MercenaryVolunteerDespawnDelay", this.volunteerDespawnDelay);
        if (this.patrolCenter != null) output.store("MercenaryPatrol", BlockPos.CODEC, this.patrolCenter);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        EntityReference<LivingEntity> owner =
                EntityReference.readWithOldOwnerConversion(input, "MercenaryOwner", this.level());
        this.entityData.set(OWNER, Optional.ofNullable(owner));
        this.entityData.set(TAMED, input.getBooleanOr("MercenaryTamed", owner != null));
        this.entityData.set(FOLLOWING, input.getBooleanOr("MercenaryFollowing", this.isTame()));
        this.entityData.set(SKIN_VARIANT, input.getStringOr("MercenaryVariant", "plains"));
        this.volunteerDespawnDelay = input.getIntOr("MercenaryVolunteerDespawnDelay", -1);
        this.patrolCenter = input.read("MercenaryPatrol", BlockPos.CODEC).orElse(null);
        if (!this.getMainHandItem().isEmpty()) this.setGuaranteedDrop(EquipmentSlot.MAINHAND);
    }
}


