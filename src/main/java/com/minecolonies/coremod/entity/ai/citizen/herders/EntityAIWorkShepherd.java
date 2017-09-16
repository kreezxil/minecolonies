package com.minecolonies.coremod.entity.ai.citizen.herders;

import com.minecolonies.api.util.constant.ToolType;
import com.minecolonies.coremod.colony.jobs.JobShepherd;
import com.minecolonies.coremod.entity.ai.util.AIState;
import com.minecolonies.coremod.entity.ai.util.AITarget;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.minecolonies.coremod.entity.ai.util.AIState.*;

/**
 * Created by Asher on 15/9/17.
 */
public class EntityAIWorkShepherd extends AbstractEntityAIHerder<JobShepherd, EntitySheep>
{
    /**
     * Experience given per sheep sheared.
     */
    protected static final double EXP_PER_SHEEP = 5.0;

    /**
     * Max amount of animals per Hut Level.
     */
    private static final int MAX_ANIMALS_PER_LEVEL = 2;

    /**
     * Creates the abstract part of the AI.
     * Always use this constructor!
     *
     * @param job the job to fulfill
     */
    public EntityAIWorkShepherd(@NotNull final JobShepherd job)
    {
        super(job, MAX_ANIMALS_PER_LEVEL);
        super.registerTargets(
          new AITarget(SHEPHERD_SHEAR, this::shearSheep)
        );
    }

    /**
     * Decides what job the shephered should switch to, breeding, butchering, or shearing.
     *
     * @return the next AIState the shepherd should switch to, after executing this method.
     */
    @Override
    public AIState decideWhatToDo()
    {
        setDelay(40);

        final List<EntitySheep> animals = new ArrayList<>(getAnimals());

        if (animals.isEmpty())
        {
            setDelay(100);
            return HERDER_DECIDE;
        }

        worker.setLatestStatus(new TextComponentTranslation("com.minecolonies.coremod.status.herder.deciding"));

        final EntitySheep shearingSheep = animals.stream().filter(sheepie -> !sheepie.getSheared()).findFirst().orElse(null);

        final int numOfBreedableSheep = animals.stream().filter(sheepie -> sheepie.getGrowingAge() == 0).toArray().length;

        if (maxAnimals())
        {
            return HERDER_BUTCHER;
        }
        else if (shearingSheep != null && !walkToAnimal(shearingSheep))
        {
            return SHEPHERD_SHEAR;
        }
        else if (numOfBreedableSheep >= NUM_OF_ANIMALS_TO_BREED)
        {
            return HERDER_BREED;
        }
        return HERDER_DECIDE;
    }

    @Override
    public List<EntitySheep> getAnimals()
    {
        return searchForAnimals(EntitySheep.class);
    }

    /**
     * Shears a sheep, with a chance of dying it!
     *
     * @return the wanted AiState
     */
    private AIState shearSheep()
    {
        worker.setLatestStatus(new TextComponentTranslation("com.minecolonies.coremod.status.shepherd.shearing"));

        final List<EntitySheep> sheeps = getAnimals();

        if (sheeps.isEmpty())
        {
            return HERDER_DECIDE;
        }

        if (!equipTool(ToolType.SHEARS))
        {
            return PREPARING;
        }

        final EntitySheep sheep = sheeps.stream().filter(sheepie -> !sheepie.getSheared()).findFirst().orElse(null);

        if (worker.getHeldItemMainhand() != null && sheep != null)
        {
            final List<ItemStack> items = sheep.onSheared(worker.getHeldItemMainhand(),
              worker.worldObj,
              worker.getPosition(),
              net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(net.minecraft.init.Enchantments.FORTUNE, worker.getHeldItemMainhand()));

            dyeSheepChance(sheep);

            worker.getHeldItemMainhand().damageItem(1, worker);

            worker.addExperience(EXP_PER_SHEEP);

            for (final ItemStack item : items)
            {
                worker.getInventoryCitizen().addItemStackToInventory(item);
            }
        }

        incrementActionsDone();

        return HERDER_DECIDE;
    }

    /**
     * Possibly dyes a sheep based on their Worker Hut Level
     */
    private void dyeSheepChance(final EntitySheep sheep)
    {
        if (worker.getWorkBuilding() != null)
        {
            final int chanceToDye = worker.getWorkBuilding().getBuildingLevel();

            final int rand = world.rand.nextInt(100);

            if (rand <= chanceToDye)
            {
                final int dyeInt = world.rand.nextInt(15);
                sheep.setFleeceColor(EnumDyeColor.byMetadata(dyeInt));
            }
        }
    }
}
