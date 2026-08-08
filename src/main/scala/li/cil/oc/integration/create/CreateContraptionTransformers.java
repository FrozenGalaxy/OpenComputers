package li.cil.oc.integration.create;

import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries;
import com.simibubi.create.content.contraptions.StructureTransform;
import li.cil.oc.common.block.SimpleBlock;
import li.cil.oc.common.blockentity.BlockEntityTypes;
import li.cil.oc.common.blockentity.traits.Rotatable;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Create transformations for OC blocks. This got a bit out of hand. */
final class CreateContraptionTransformers {
    private static final String PITCH = "pitch";
    private static final String YAW = "yaw";
    private static final String FACING = "facing";
    private static final String MOUNT = "mount";

    private CreateContraptionTransformers() {
    }

    static void register() {
        // I tried to make a list of these. The registry loop was less typing.
        // Maybe this works.
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof SimpleBlock) {
                MovedBlockTransformerRegistries.BLOCK_TRANSFORMERS.register(
                        block, CreateContraptionTransformers::transformBlock);
            }
        }

        registerBlockEntity(BlockEntityTypes.HOLOGRAM.get());
        registerBlockEntity(BlockEntityTypes.PRINT.get());
        registerBlockEntity(BlockEntityTypes.ROBOT.get());
    }

    private static void registerBlockEntity(final BlockEntityType<?> type) {
        MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(
                type, CreateContraptionTransformers::transformBlockEntity);
    }

    static BlockState transformBlock(final BlockState state, final StructureTransform transform) {
        Direction pitch = getDirection(state, PITCH);
        Direction yaw = getDirection(state, YAW);
        if (pitch != null && yaw != null) {
            return transformPitchAndYaw(state, pitch, yaw, transform);
        }

        Direction facing = getDirection(state, FACING);
        if (facing != null) {
            Direction transformed = transformDirection(facing, transform);
            if (hasDirection(state, FACING, transformed)) {
                return setDirection(state, FACING, transformed);
            }
        }

        Direction mount = getDirection(state, MOUNT);
        if (mount != null) {
            Direction transformed = transformDirection(mount, transform);
            if (hasDirection(state, MOUNT, transformed)) {
                return setDirection(state, MOUNT, transformed);
            }
        }

        return state;
    }

    private static BlockState transformPitchAndYaw(final BlockState state, final Direction pitch,
                                                    final Direction yaw, final StructureTransform transform) {
        // Pitch is either actually pitch or NORTH pretending not to be pitch.
        // Hmmm. Like this?
        Direction forward = yaw;
        Direction up = Direction.UP;
        if (pitch.getAxis().isVertical()) {
            forward = pitch;
            up = yaw;
        }
        forward = transformDirection(forward, transform);
        up = transformDirection(up, transform);

        if (forward.getAxis().isVertical()) {
            BlockState result = setDirection(state, PITCH, forward);
            if (hasDirection(result, YAW, up)) {
                result = setDirection(result, YAW, up);
            }
            return result;
        }

        // PropertyRotatable.Pitch only allows UP, DOWN, or NORTH. Horizontal
        // orientation lives in Yaw, so NORTH is the "no pitch" sentinel here.
        // FUCK YOU MINECRAFT! Why is NORTH the "not really pitch" direction?
        BlockState result = setDirection(state, PITCH, Direction.NORTH);
        if (hasDirection(result, YAW, forward)) {
            result = setDirection(result, YAW, forward);
        }
        return result;
    }

    private static Direction transformDirection(final Direction direction, final StructureTransform transform) {
        Direction result = direction;
        if (transform.mirror != null && transform.mirror != Mirror.NONE) {
            result = transform.mirrorFacing(result);
        }
        // Create calls this with Rotation.NONE sometimes, which is fine.
        if (transform.rotation != null && transform.rotationAxis != null
                && transform.rotation.ordinal() != 0) {
            result = transform.rotateFacing(result);
        }
        return result;
    }

    private static void transformBlockEntity(final BlockEntity blockEntity, final StructureTransform transform) {
        if (!(blockEntity instanceof Rotatable rotatable)) {
            return;
        }

        Direction oldPitch = rotatable.pitch();
        Direction oldYaw = rotatable.yaw();
        Direction forward = oldYaw;
        Direction up = Direction.UP;
        if (oldPitch.getAxis().isVertical()) {
            forward = oldPitch;
            up = oldYaw;
        }
        forward = transformDirection(forward, transform);
        up = transformDirection(up, transform);

        // There is probably a nicer way to do this. This is the way that is
        // least likely to make the old block orientation code angry.
        if (forward.getAxis().isVertical()) {
            rotatable.setFromPitchAndYaw(forward,
                    up.getAxis().isHorizontal() ? up : oldYaw);
        } else {
            rotatable.setFromPitchAndYaw(Direction.NORTH, forward);
        }
    }

    private static Direction getDirection(final BlockState state, final String name) {
        final Property<?> property = findProperty(state, name);
        if (property == null) {
            return null;
        }
        return state.getValue(directionProperty(property));
    }

    private static boolean hasDirection(final BlockState state, final String name, final Direction value) {
        final Property<?> property = findProperty(state, name);
        return property != null && property.getPossibleValues().contains(value);
    }

    private static BlockState setDirection(final BlockState state, final String name, final Direction value) {
        return state.setValue(directionProperty(findProperty(state, name)), value);
    }

    private static Property<?> findProperty(final BlockState state, final String name) {
        for (Property<?> property : state.getProperties()) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Property<Direction> directionProperty(final Property<?> property) {
        return (Property<Direction>) property;
    }
}
