package mypoli.android.pet.usecase

import mypoli.android.TestUtil
import mypoli.android.pet.Pet
import mypoli.android.pet.PetAvatar
import mypoli.android.pet.PetEquipment
import mypoli.android.pet.PetItem
import mypoli.android.player.Inventory
import mypoli.android.player.InventoryPet
import mypoli.android.player.Player
import org.amshove.kluent.`should equal`
import org.amshove.kluent.shouldThrow
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

/**
 * Created by Venelin Valkov <venelin@mypoli.fun>
 * on 12/26/17.
 */
class EquipPetItemUseCaseSpek : Spek({

    describe("EquipPetItemUseCase") {

        fun executeUseCase(player: Player, petItem: PetItem) =
            EquipPetItemUseCase(TestUtil.playerRepoMock(player)).execute(
                EquipPetItemUseCase.Params(
                    petItem
                )
            )

        it("should have item in pet inventory") {
            val player = TestUtil.player().copy(
                pet = Pet(
                    "Pencho",
                    PetAvatar.ELEPHANT
                ),
                inventory = Inventory(
                    pets = setOf(
                        InventoryPet("Pencho", PetAvatar.ELEPHANT)
                    )
                )
            )
            val exec = { executeUseCase(player, PetItem.RED_HAT) }
            exec shouldThrow IllegalArgumentException::class
        }

        it("should equip item") {
            val player = TestUtil.player().copy(
                pet = Pet(
                    "Pencho",
                    PetAvatar.ELEPHANT
                ),
                inventory = Inventory(
                    pets = setOf(
                        InventoryPet("Pencho", PetAvatar.ELEPHANT, setOf(PetItem.RED_HAT))
                    )
                )
            )
            val result = executeUseCase(player, PetItem.RED_HAT)
            result.pet.equipment.hat.`should equal`(PetItem.RED_HAT)
        }

        it("should change equipped item") {
            val player = TestUtil.player().copy(
                pet = Pet(
                    "Pencho",
                    PetAvatar.ELEPHANT,
                    equipment = PetEquipment(
                        hat = PetItem.RED_WHITE_HAT
                    )
                ),
                inventory = Inventory(
                    pets = setOf(
                        InventoryPet("Pencho", PetAvatar.ELEPHANT, setOf(PetItem.RED_HAT))
                    )
                )
            )
            val result = executeUseCase(player, PetItem.RED_HAT)
            result.pet.equipment.hat.`should equal`(PetItem.RED_HAT)
        }
    }
})