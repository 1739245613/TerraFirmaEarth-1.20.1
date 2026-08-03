package com.newterraearth.tfe.world.river;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.dries007.tfc.world.river.Flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NTEHeadwaterJunctionFlowTest
{
    @Test
    void junctionFlowUsesSixteenthDirectionsToBridgeBothBranchesIntoOneSharedDirection() throws Exception
    {
        final NTEHeadwaterNetwork.Vec junction = new NTEHeadwaterNetwork.Vec(0d, 0d);
        final NTEHeadwaterNetwork.TestStream horizontal = NTEHeadwaterNetwork.testStreamFromRoute(
            List.of(
                new NTEHeadwaterNetwork.Vec(80d, 0d),
                junction,
                new NTEHeadwaterNetwork.Vec(-80d, 0d)
            ),
            78d,
            68d,
            (x, z) -> 82d
        );
        final NTEHeadwaterNetwork.TestStream vertical = NTEHeadwaterNetwork.testStreamFromRoute(
            List.of(
                new NTEHeadwaterNetwork.Vec(0d, 80d),
                junction,
                new NTEHeadwaterNetwork.Vec(0d, -80d)
            ),
            72d,
            64d,
            (x, z) -> 82d
        );

        assertTrue(NTEHeadwaterNetwork.coordinateTestStreams(horizontal, vertical));

        final Object horizontalAfter = route(horizontal);
        final Object verticalAfter = route(vertical);
        final double horizontalAlong = alongAt(horizontalAfter, junction);
        final double verticalAlong = alongAt(verticalAfter, junction);
        final Flow horizontalFar = flowAt(horizontalAfter, horizontalAlong - 5d);
        final Flow horizontalMiddle = flowAt(horizontalAfter, horizontalAlong - 3d);
        final Flow horizontalNear = flowAt(horizontalAfter, horizontalAlong - 1d);
        final Flow horizontalAfterNear = flowAt(horizontalAfter, horizontalAlong + 1d);
        final Flow horizontalAfterMiddle = flowAt(horizontalAfter, horizontalAlong + 3d);
        final Flow horizontalAfterFar = flowAt(horizontalAfter, horizontalAlong + 5d);
        final Flow verticalFar = flowAt(verticalAfter, verticalAlong - 5d);
        final Flow verticalMiddle = flowAt(verticalAfter, verticalAlong - 3d);

        assertEquals(Flow.WWW, horizontalFar);
        assertNotEquals(horizontalFar, horizontalMiddle);
        assertNotEquals(horizontalMiddle, horizontalNear);
        assertNotEquals(horizontalAfterNear, horizontalAfterMiddle);
        assertNotEquals(horizontalAfterMiddle, horizontalAfterFar);
        assertEquals(Flow.WWW, horizontalAfterFar);
        assertEquals(Flow.NNN, verticalFar);
        assertEquals(Flow.NNN, verticalMiddle);
        assertNotEquals(Flow.NONE, horizontalMiddle);
        assertNotEquals(Flow.NONE, horizontalNear);
        assertNotEquals(Flow.NONE, horizontalAfterNear);
        assertNotEquals(Flow.NONE, horizontalAfterMiddle);
    }

    private static Object route(NTEHeadwaterNetwork.TestStream stream) throws Exception
    {
        final Field headwaterField = NTEHeadwaterNetwork.TestStream.class.getDeclaredField("headwater");
        headwaterField.setAccessible(true);
        final Object headwater = headwaterField.get(stream);
        final Method route = headwater.getClass().getDeclaredMethod("route");
        route.setAccessible(true);
        return route.invoke(headwater);
    }

    private static double alongAt(Object route, NTEHeadwaterNetwork.Vec point) throws Exception
    {
        final Method nearest = route.getClass().getDeclaredMethod("nearestProjection", NTEHeadwaterNetwork.Vec.class);
        nearest.setAccessible(true);
        final Object projection = nearest.invoke(route, point);
        final Method along = projection.getClass().getDeclaredMethod("along");
        along.setAccessible(true);
        return (double) along.invoke(projection);
    }

    private static Flow flowAt(Object route, double along) throws Exception
    {
        final Method directionAt = route.getClass().getDeclaredMethod("flowDirectionAtAlong", double.class);
        directionAt.setAccessible(true);
        final NTEHeadwaterNetwork.Vec direction = (NTEHeadwaterNetwork.Vec) directionAt.invoke(route, along);
        final Flow routeFlow = Flow.fromAngle(Math.atan2(-direction.z(), direction.x()));
        final Method flowAt = route.getClass().getDeclaredMethod("junctionFlowAtAlong", double.class, Flow.class);
        flowAt.setAccessible(true);
        return (Flow) flowAt.invoke(route, along, routeFlow);
    }
}
