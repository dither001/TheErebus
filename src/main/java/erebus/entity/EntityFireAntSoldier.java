package erebus.entity;

import erebus.ModSounds;
import erebus.core.handler.configs.ConfigHandler;
import erebus.entity.ai.EntityAIErebusAttackMelee;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class EntityFireAntSoldier extends EntityMob {

	public EntityFireAntSoldier(World world) {
		super(world);
		stepHeight = 0.1F;
		isImmuneToFire = true;
		setSize(0.9F, 0.5F);
	}

	@Override
	protected void initEntityAI() {
		tasks.addTask(0, new EntityAISwimming(this));
		tasks.addTask(1, new EntityFireAntSoldier.AIFireballAttackBig(this));
		tasks.addTask(2, new EntityAIErebusAttackMelee(this, 0.5D, true));
		tasks.addTask(3, new EntityAIWander(this, 0.5D));
		tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
		tasks.addTask(5, new EntityAILookIdle(this));
		targetTasks.addTask(0, new EntityAIHurtByTarget(this, true, new Class[0]));
		targetTasks.addTask(1, new EntityAINearestAttackableTarget(this, EntityPlayer.class, true));
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
		getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(ConfigHandler.INSTANCE.mobHealthMultipier < 2 ? 20D : 20D * ConfigHandler.INSTANCE.mobHealthMultipier);
		getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(ConfigHandler.INSTANCE.mobAttackDamageMultiplier < 2 ? 4D : 4D * ConfigHandler.INSTANCE.mobAttackDamageMultiplier);
		getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16.0D);
	}

	@Override
	public EnumCreatureAttribute getCreatureAttribute() {
		return EnumCreatureAttribute.ARTHROPOD;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.FIRE_ANT_SOUND;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.FIRE_ANT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SQUISH;
	}

	@Override
	protected void playStepSound(BlockPos pos, Block blockIn) {
		this.playSound(SoundEvents.ENTITY_SPIDER_STEP, 0.15F, 1.0F);
	}

	@Override
	protected void dropFewItems(boolean recentlyHit, int looting) {
		if (recentlyHit) {
			entityDropItem(new ItemStack(Items.BLAZE_POWDER, rand.nextInt(1) + 1 + looting, 0), 0.0F);
			if (rand.nextInt(5) == 0)
				entityDropItem(new ItemStack(Items.BLAZE_ROD, rand.nextInt(1) + 1 + looting, 0), 0.0F);
		}
	}

	public boolean isClimbing() {
		return !onGround && isOnLadder();
	}

	@Override
	public boolean isOnLadder() {
		return collidedHorizontally;
	}

	@Override
	public boolean getCanSpawnHere() {
		float light = getBrightness();
		if (light >= 0F)
			return isNotColliding();
		return super.getCanSpawnHere();
	}

	@Override
	public int getMaxSpawnedInChunk() {
		return 5;
	}

	static class AIFireballAttackBig extends EntityAIBase {
		private final EntityFireAntSoldier fire_ant;
		private int attackStep;
		private int attackTime;

		public AIFireballAttackBig(EntityFireAntSoldier fire_antIn) {
			fire_ant = fire_antIn;
			setMutexBits(3);
		}

		@Override
		public boolean shouldExecute() {
			EntityLivingBase entitylivingbase = fire_ant.getAttackTarget();
			return entitylivingbase != null && entitylivingbase.isEntityAlive();
		}

		@Override
		public void startExecuting() {
			attackStep = 0;
		}

		@Override
		public void updateTask() {
			--attackTime;
			EntityLivingBase entitylivingbase = fire_ant.getAttackTarget();
			double d0 = fire_ant.getDistanceSq(entitylivingbase);
			if (d0 < 4.0D) {
				if (attackTime <= 0) {
					attackTime = 20;
					fire_ant.attackEntityAsMob(entitylivingbase);
				}
				fire_ant.getMoveHelper().setMoveTo(entitylivingbase.posX, entitylivingbase.posY, entitylivingbase.posZ, 0.6D);
			} else if (d0 < 256.0D) {
				double d1 = entitylivingbase.posX - fire_ant.posX;
				double d2 = entitylivingbase.getEntityBoundingBox().minY + (double) (entitylivingbase.height / 2.0F) - (fire_ant.posY + (double) (fire_ant.height / 2.0F));
				double d3 = entitylivingbase.posZ - fire_ant.posZ;
				if (attackTime == 0) {
					++attackStep;
					if (attackStep <= 4) 
						attackTime = 60;
					else {
						attackTime = 100;
						attackStep = 0;
					}
					if (attackStep > 1) {
						float f = MathHelper.sqrt(MathHelper.sqrt(d0)) * 0.5F;
						fire_ant.getEntityWorld().playEvent((EntityPlayer) null, 1009, new BlockPos((int) fire_ant.posX, (int) fire_ant.posY, (int) fire_ant.posZ), 0);
						EntityLargeFireball fireball = new EntityLargeFireball(fire_ant.getEntityWorld(), fire_ant, d1 + fire_ant.getRNG().nextGaussian() * (double) f, d2, d3 + fire_ant.getRNG().nextGaussian() * (double) f);
						fireball.posY = fire_ant.posY + (double) (fire_ant.height / 2.0F) + 0.5D;
						fire_ant.getEntityWorld().spawnEntity(fireball);
					}
				}
				fire_ant.getLookHelper().setLookPositionWithEntity(entitylivingbase, 10.0F, 10.0F);
				fire_ant.getNavigator().clearPath();
				fire_ant.getMoveHelper().setMoveTo(entitylivingbase.posX, entitylivingbase.posY, entitylivingbase.posZ, 0.6D);
			}
			super.updateTask();
		}
	}
}
