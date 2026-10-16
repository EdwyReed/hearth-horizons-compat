package dev.hearthhorizons.qol.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.ColorParticleOption;

/** Uses the standard translucent particle pass supported by Iris/Photon.
 * Bubbles rise in air instead of being removed when they leave water.
 */
public final class SodaBubbleParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private SodaBubbleParticle(ColorParticleOption colour,ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites) {
        super(level,x,y,z);this.sprites=sprites;
        lifetime=14+random.nextInt(8);xd=dx;yd=.018+dy;zd=dz;
        quadSize=.035F+random.nextFloat()*.025F;
        rCol=.7F+.3F*colour.getRed();gCol=.7F+.3F*colour.getGreen();bCol=.7F+.3F*colour.getBlue();
        hasPhysics=false;setSpriteFromAge(sprites);
    }
    @Override public void tick() {
        xo=x;yo=y;zo=z;
        if(age++>=lifetime) { remove(); return; }
        move(xd,yd,zd);alpha=1F-(float)age/lifetime;setSpriteFromAge(sprites);
    }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    public static final class Provider implements ParticleProvider<ColorParticleOption> {
        private final SpriteSet sprites;
        public Provider(SpriteSet sprites) { this.sprites=sprites; }
        @Override public Particle createParticle(ColorParticleOption colour,ClientLevel level,double x,double y,double z,double dx,double dy,double dz) {
            return new SodaBubbleParticle(colour,level,x,y,z,dx,dy,dz,sprites);
        }
    }
}
