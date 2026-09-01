package com.newterraearth.tfe.mixin;

import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.region.Region;
import net.dries007.tfc.world.region.RegionGenerator;
import net.dries007.tfc.world.region.Units;
import net.dries007.tfc.world.settings.RockSettings;
import net.dries007.tfc.world.settings.Settings;

import com.newterraearth.tfe.world.NTESeed;
import com.newterraearth.tfe.world.NTEBiomeNoise;
import com.newterraearth.tfe.world.noise.NTECellular2D;
import com.newterraearth.tfe.world.region.NTERegionGeneratorAccess;
import com.newterraearth.tfe.world.region.NTERegionNoise;

@Mixin(value = RegionGenerator.class, remap = false)
public abstract class RegionGeneratorMixin implements NTERegionGeneratorAccess
{
    @Shadow @Final private long seed;
    @Shadow @Final public Cellular2D cellNoise;

    @Unique private Settings tfe$settings;
    @Unique private Noise2D tfe$oceanicInfluenceNoise;
    @Unique private Noise2D tfe$rainfallVarianceNoise;
    @Unique private Noise2D tfe$hotSpotAgeNoise;
    @Unique private Noise2D tfe$hotSpotIntensityNoise;
    @Unique private Cellular2D tfe$plateRegionNoise;
    @Unique private NTECellular2D tfe$exactCellNoise;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void tfe$initExtraRegionNoise(Settings settings, RandomSource random, CallbackInfo ci)
    {
        this.tfe$settings = settings;
        final int cellSeed = ((Cellular2DAccessorMixin) (Object) cellNoise).tfe$getSeed();
        this.tfe$exactCellNoise = NTECellular2D.fromHashedSeed(cellSeed)
            .spread(1d / Units.CELL_WIDTH_IN_GRID);
        tfe$reinitExtraRegionNoise(seed);
    }

    @Unique
    private void tfe$reinitExtraRegionNoise(long rootLevelSeed)
    {
        final NTESeed rootSeed = NTESeed.of(rootLevelSeed);
        rootSeed.next(); // cellNoise
        rootSeed.next(); // continentNoise
        rootSeed.next(); // temperatureNoise
        this.tfe$oceanicInfluenceNoise = new OpenSimplex2D(rootSeed.next())
            .spread(0.02f);
        rootSeed.next(); // rainfallNoise
        this.tfe$rainfallVarianceNoise = new OpenSimplex2D(rootSeed.next())
            .octaves(2)
            .spread(0.1f)
            .scaled(0f, 20f);
        this.tfe$hotSpotAgeNoise = NTERegionNoise.hotSpotAge(rootSeed.seed()).spread(128);
        this.tfe$hotSpotIntensityNoise = NTERegionNoise.hotSpotIntensity(rootSeed.seed()).spread(128);
        this.tfe$plateRegionNoise = NTERegionNoise.plateRegions(rootSeed.seed()).spread(128);
    }

    @Override
    public void nte$setRootLevelSeed(long rootLevelSeed)
    {
        NTEBiomeNoise.registerRegionCellSeed(rootLevelSeed, ((Cellular2DAccessorMixin) (Object) cellNoise).tfe$getSeed());
        tfe$reinitExtraRegionNoise(rootLevelSeed);
    }

    @Override
    public Noise2D nte$getHotSpotAgeNoise()
    {
        return tfe$hotSpotAgeNoise;
    }

    @Override
    public Noise2D nte$getHotSpotIntensityNoise()
    {
        return tfe$hotSpotIntensityNoise;
    }

    @Override
    public Cellular2D nte$getPlateRegionNoise()
    {
        return tfe$plateRegionNoise;
    }

    @Override
    public Noise2D nte$getOceanicInfluenceNoise()
    {
        return tfe$oceanicInfluenceNoise;
    }

    @Override
    public Noise2D nte$getRainfallVarianceNoise()
    {
        return tfe$rainfallVarianceNoise;
    }

    @Override
    public Settings nte$getSettings()
    {
        return tfe$settings;
    }

    @Override
    public float nte$continentFactor(int gridX, int gridZ)
    {
        return 1f;
    }

    @Override
    public double nte$getDivergence(int gridX, int gridZ)
    {
        final NTECellular2D.Cell thisCell = tfe$exactCellNoise.cell(gridX, gridZ);
        final NTECellular2D.Cell adjacentCell = tfe$exactCellNoise.cell(thisCell.nx(), thisCell.ny());

        final double convergeX = nte$getDivergence(thisCell.x(), adjacentCell.x(), nte$getVX(thisCell.noise()), nte$getVX(adjacentCell.noise()));
        final double convergeZ = nte$getDivergence(thisCell.y(), adjacentCell.y(), nte$getVY(thisCell.noise()), nte$getVY(adjacentCell.noise()));
        return convergeX + convergeZ;
    }

    private double nte$getDivergence(double firstCoordinate, double secondCoordinate, double firstVelocity, double secondVelocity)
    {
        final double deltaVelocity = secondVelocity - firstVelocity;
        return firstCoordinate < secondCoordinate ? deltaVelocity : -deltaVelocity;
    }

    private double nte$getVX(double noise)
    {
        return nte$hashDouble(noise, 95274);
    }

    private double nte$getVY(double noise)
    {
        return nte$hashDouble(noise, 894824);
    }

    private double nte$hashDouble(double input, int index)
    {
        long value = Double.doubleToLongBits(input) + index;
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (value >>> 11) * 0x1.0p-53;
    }

    @Override
    public RockSettings nte$getSurfaceRock(Region.Point point)
    {
        return tfe$settings.rockLayerSettings().sampleAtLayer(point.rock, 0);
    }
}
