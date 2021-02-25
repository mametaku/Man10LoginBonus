package red.man10.man10loginbonus

import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*


object Utility {
    var prefix = "&e&l[&d&lMan10LoginBonus&e&l]&f".replace("&".toRegex(), "§")

    //ホバーテキスト、クリックイベント
    fun sendHoverText(p: Player, text: String, hoverText: String, command: String) {
        //////////////////////////////////////////
        //      ホバーテキストとイベントを作成する
        val hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ComponentBuilder(hoverText).create())

        //////////////////////////////////////////
        //   クリックイベントを作成する
        val clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/$command")
        val message = ComponentBuilder(prefix +text).event(hoverEvent).event(clickEvent).create()
        p.spigot().sendMessage(*message)
    }
    fun inventoryToString(inventory: Inventory): String? {
        try {
            val str = ByteArrayOutputStream()
            val data = BukkitObjectOutputStream(str)
            data.writeInt(inventory.size)
            data.writeObject(inventory.contents)
            for (i in 0 until inventory.size) {
                data.writeObject(inventory.getItem(i))
            }
            data.close()
            return Base64.getEncoder().encodeToString(str.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun stringToInventory(inventoryData: String?): Inventory? {
        try {
            val stream = ByteArrayInputStream(Base64.getDecoder().decode(inventoryData))
            val data = BukkitObjectInputStream(stream)
            val inventory = Bukkit.createInventory(null, data.readInt(), data.readObject().toString())
            for (i in 0 until inventory.size) {
                inventory.setItem(i, data.readObject() as ItemStack)
            }
            data.close()
            return inventory
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    //prefix付きのメッセージ
    fun sendMessage(player: Player, message: String) {
        player.sendMessage("${prefix} $message")
    }

}