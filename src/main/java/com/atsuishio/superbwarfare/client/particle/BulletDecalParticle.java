package com.atsuishio.superbwarfare.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * @author Forked from MrCrayfish, continued by Timeless devs
 * Code based on TaC-Z
 */
public class BulletDecalParticle extends TextureSheetParticle {

    private final Direction direction;
    private final BlockPos pos;
    private int uOffset;
    private int vOffset;
    private float textureDensity;

    public BulletDecalParticle(ClientLevel level, double x, double y, double z, Direction direction, BlockPos pos) {
        this(level, x, y, z, direction, pos, 0f, 0f, 0f);
    }

    public BulletDecalParticle(ClientLevel level, double x, double y, double z, Direction direction, BlockPos pos, float rCol, float gCol, float bCol) {
        super(level, x, y, z);
        this.setSprite(this.getSprite(pos));
        this.direction = direction;
        this.pos = pos;
        this.lifetime = 200;
        this.hasPhysics = false;
        this.gravity = 0;
        this.quadSize = 0.05F;

        if (shouldRemove()) {
            this.remove();
        }

        this.rCol = rCol;
        this.gCol = gCol;
        this.bCol = bCol;

        this.alpha = 0.9F;
    }

    @Override
    protected void setSprite(@NotNull TextureAtlasSprite sprite) {
        super.setSprite(sprite);
        this.uOffset = this.random.nextInt(16);
        this.vOffset = this.random.nextInt(16);
        this.textureDensity = (sprite.getU1() - sprite.getU0()) / 16F;
    }

    private TextureAtlasSprite getSprite(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        Level clientLevel = minecraft.level;
        if (clientLevel != null) {
            BlockState state = clientLevel.getBlockState(pos);
            return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getTexture(state, clientLevel, pos);
        }
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation());
    }

    @Override
    protected float getU0() {
        return this.sprite.getU0() + this.uOffset * this.textureDensity;
    }

    @Override
    protected float getV0() {
        return this.sprite.getV0() + this.vOffset * this.textureDensity;
    }

    @Override
    protected float getU1() {
        return this.getU0() + this.textureDensity;
    }

    @Override
    protected float getV1() {
        return this.getV0() + this.textureDensity;
    }

    @Override
    public void tick() {
        super.tick();
        if (shouldRemove()) {
            this.remove();
        }
    }

    @Override
    public void render(@NotNull VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        Vec3 view = renderInfo.getPosition();
        float particleX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - view.x());
        float particleY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - view.y());
        float particleZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - view.z());
        Quaternionf quaternion = this.direction.getRotation();
        Vector3f[] points = new Vector3f[]{
                // Y 值稍微大一点点，防止 z-fight
                new Vector3f(-1, 0.01F, -1),
                new Vector3f(-1, 0.01F, 1),
                new Vector3f(1, 0.01F, 1),
                new Vector3f(1, 0.01F, -1)
        };
        float scale = this.getQuadSize(partialTicks);

        for (int i = 0; i < 4; ++i) {
            Vector3f vector3f = points[i];
            vector3f.rotate(quaternion);
            vector3f.mul(scale);
            vector3f.add(particleX, particleY, particleZ);
        }

        // UV 坐标
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();

        // 0 - 30 tick 内，从 15 亮度到 0 亮度
        int light = Math.max(15 - this.age / 2, 0);
        int lightColor = LightTexture.FULL_BRIGHT;

        // 颜色，逐渐渐变到 0 0 0，也就是黑色
        float colorPercent = light / 15.0f;
        float red = this.rCol * colorPercent;
        float green = this.gCol * colorPercent;
        float blue = this.bCol * colorPercent;

        // 透明度，逐渐变成 0，也就是透明
        double threshold = 0.98 * this.lifetime;
        float fade = 1.0f - (float) (Math.max(this.age - threshold, 0) / (this.lifetime - threshold));
        float alphaFade = this.alpha * fade;

        buffer.addVertex(points[0].x(), points[0].y(), points[0].z()).setUv(u1, v1).setColor(red, green, blue, alphaFade).setLight(lightColor);
        buffer.addVertex(points[1].x(), points[1].y(), points[1].z()).setUv(u1, v0).setColor(red, green, blue, alphaFade).setLight(lightColor);
        buffer.addVertex(points[2].x(), points[2].y(), points[2].z()).setUv(u0, v0).setColor(red, green, blue, alphaFade).setLight(lightColor);
        buffer.addVertex(points[3].x(), points[3].y(), points[3].z()).setUv(u0, v1).setColor(red, green, blue, alphaFade).setLight(lightColor);
    }

    private boolean shouldRemove() {
        final BlockState blockState = this.level.getBlockState(this.pos);
        if (blockState.isAir()) {
            return true;
        } else {
            // 阻止弹孔在与方块不构成有效附着时继续渲染
            VoxelShape shape = blockState.getCollisionShape(this.level, this.pos);
            if (shape.isEmpty()) {
                return true;
            }
            AABB baseBlockBoundingBox = shape.bounds();
            AABB blockBoundingBox = baseBlockBoundingBox.move(this.pos);
            return !blockBoundingBox.intersects(
                    this.x - 0.1, this.y - 0.1, this.z - 0.1,
                    this.x + 0.1, this.y + 0.1, this.z + 0.1);
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<BulletDecalOption> {
        public Provider() {
        }

        @Override
        public BulletDecalParticle createParticle(@NotNull BulletDecalOption option, @NotNull ClientLevel world, double x, double y, double z, double pXSpeed, double pYSpeed, double pZSpeed) {
            return new BulletDecalParticle(world, x, y, z, option.getDirection(), option.getPos(), option.getRed(), option.getGreen(), option.getBlue());
        }
    }
}