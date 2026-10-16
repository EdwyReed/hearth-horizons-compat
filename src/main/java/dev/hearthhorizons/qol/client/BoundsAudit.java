package dev.hearthhorizons.qol.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Opt-in geometry instrumentation. No instances or wrappers are created in normal play. */
public final class BoundsAudit implements MultiBufferSource {
    private final MultiBufferSource delegate;
    private final Matrix4f inverse;
    private final Vector3f position = new Vector3f();
    private double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
    private double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
    public BoundsAudit(MultiBufferSource delegate, Matrix4f initial) { this.delegate = delegate; this.inverse = new Matrix4f(initial).invert(); }
    @Override public VertexConsumer getBuffer(RenderType type) {
        VertexConsumer target = delegate.getBuffer(type);
        return new VertexConsumer() {
            public VertexConsumer addVertex(float x, float y, float z) {
                inverse.transformPosition(position.set(x, y, z));
                minX = Math.min(minX, position.x); minY = Math.min(minY, position.y); minZ = Math.min(minZ, position.z);
                maxX = Math.max(maxX, position.x); maxY = Math.max(maxY, position.y); maxZ = Math.max(maxZ, position.z);
                target.addVertex(x, y, z); return this;
            }
            public VertexConsumer setColor(int r, int g, int b, int a) { target.setColor(r, g, b, a); return this; }
            public VertexConsumer setUv(float u, float v) { target.setUv(u, v); return this; }
            public VertexConsumer setUv1(int u, int v) { target.setUv1(u, v); return this; }
            public VertexConsumer setUv2(int u, int v) { target.setUv2(u, v); return this; }
            public VertexConsumer setNormal(float x, float y, float z) { target.setNormal(x, y, z); return this; }
        };
    }
    public String report() { return String.format(java.util.Locale.ROOT, "min=(%.5f,%.5f,%.5f) max=(%.5f,%.5f,%.5f)", minX, minY, minZ, maxX, maxY, maxZ); }
}
