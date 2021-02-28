package red.man10.man10loginbonus

import javafx.beans.binding.IntegerBinding
import org.bson.Document
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerEvent
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.File
import java.util.*


class Man10LoginBonus : JavaPlugin(), Listener {
    var prefix = "&e&l[&d&lMan10LoginBonus&e&l]&f".replace("&".toRegex(), "§")
    val PLName = "Man10LoginBonus"
    var data = MongoDBManager(this, "user_logged_in_time")
    val list = mutableListOf(2, 3, 4, 5, 6, 11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 29, 30, 31, 32, 33, 38, 39, 40, 41, 42)
    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()
        Bukkit.getPluginManager().registerEvents(this, this)
        // config.ymlを読み込みます。
        val config = config
        reloadConfig()
        getCommand("mlb")!!.setExecutor(this)
        if (!config.getBoolean("mode")) {
            logger.info("$prefix is not run.")
        }
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    fun LoginEvent(event: PlayerLoginEvent?) {
        val config = config
        if (!config.getBoolean("mode")) return
        Bukkit.getScheduler().runTaskAsynchronously(this, Runnable {
            try {
                setData(event as PlayerEvent?)
            } catch (e: Exception) {
                Bukkit.getLogger().info(e.message)
                println(e.message)
            }
        })
    }

    fun setData(event: PlayerEvent?) {
        val p = event?.player
        val uuid = p?.uniqueId.toString()
        val player = p?.name
        val date = System.currentTimeMillis()
        var count = "0"
        val doc = Document()
        doc.append("uuid", uuid)
        val result = data.queryFind(doc)
        if(result.isEmpty()) {
            doc.append("mcid", player)
            doc.append("logged_in_time", date.toString())
            doc.append("count", count)
            data.queryInsertOne(doc)
        }
        val result1 = data.queryFind(doc)
        val parsed: JSONObject = JSONParser().parse(result1[0].toJson()) as JSONObject
        val beforetime = parsed["logged_in_time"] as String
        val oneDateTime = 1000 * 60 * 60 * 24.toLong()
        if (date - beforetime.toLong()  >= oneDateTime){
            count = parsed["count"] as String
            try {
                var intCount = count.toInt()
                intCount = intCount++
                count = intCount.toString()
            }catch(e: NumberFormatException) {}
            val filter = Document().append("uuid", uuid)
            val update = Document()
            update.append("count", count)
            update.append("logged_in_time",date.toString())
            data.queryUpdateOne(filter,update)
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val p = sender as Player
        if (command.name.equals("man10loginbonus", ignoreCase = true) || command.name.equals("mlb", ignoreCase = true)) {
            if (args.isEmpty()) {
                help(p)
                return false
            }
            if (args[0].equals("addbonus", ignoreCase = true)) {
                if (!sender.hasPermission("man10.loginbonus.addbonus")) return false
                val cal = Calendar.getInstance()
                val month = cal.get(Calendar.MONTH) + 1
                Bukkit.broadcastMessage(month.toString())
                val inv = Bukkit.createInventory(null, 54, "${prefix}${month}月の報酬")
                val con = File("plugins/${PLName}/${month}.yml")
                if (!con.exists()) {
                    con.createNewFile()
                    openbonusinventory(p,inv)
                } else {
                    val config = YamlConfiguration.loadConfiguration(con)
                    for (i in 0..54) {
                        if (config.isSet("saveinv.$i")) {
                            inv.setItem(i, config.getItemStack("saveinv.$i"))
                            openbonusinventory(p,inv)
                        }
                    }
                }
            }
            if (args[0].equals("reload", ignoreCase = true)) {
                if (!sender.hasPermission("man10.loginbonus.reload")) {
                    sender.sendMessage(prefix + "あなたには権限がありません")
                    return false
                }
                reloadConfig()
                sender.sendMessage(prefix + "コンフィグをリロードしました")
                return false
            }
            if (args[0].equals("help", ignoreCase = true)) {
                help(sender)
                return false
            }
            if (args[0].equals("set", ignoreCase = true)) {
                if (args.size == 2) {
                    if (args[1].toInt() in 1..12) {
                        openSettingInventory(p, args[1])
                        return false
                    }
                    p.sendMessage("§6数値は1～12の間にしてください！")
                    return false
                }
                p.sendMessage("§6/mlb set [month/月] 報酬のセット")
                return false
            }
        }
        return false
    }

    @EventHandler
    fun InventoryClick(e: InventoryClickEvent) {
        val p = e.whoClicked
        val slot = e.slot
        val cal = Calendar.getInstance()
        val month1 = cal.get(Calendar.MONTH) + 1
        if (e.view.title == "${prefix}${month1}月の報酬"){
            if (slot in 47..51){
                val doc = Document()
                val result1 = data.queryFind(doc)
                val parsed: JSONObject = JSONParser().parse(result1[0].toJson()) as JSONObject
                val count = parsed["count"] as Long
                for (i in list){
                    p.inventory.addItem(e.view.getItem(i))
                }
                p.closeInventory()
                return
            }
        }
        if (e.view.title.length != 41 && e.view.title.length != 42) return
        val month = e.view.title.substring(31, e.view.title.length - 10).toInt()
        when (e.view.title) {
            "${prefix}${month}月のボーナス報酬設定" ->
                if (slot in 47..51) {
                    p.closeInventory()
                }
        }
    }

    @EventHandler
    fun InventoryCloseEvent(e: InventoryCloseEvent) {
        Bukkit.broadcastMessage(e.view.title.length.toString())
        if (e.view.title.length != 41 && e.view.title.length != 42) return
        val month = e.view.title.substring(31, e.view.title.length - 10).toInt()
        val con = File("plugins/Man10LoginBonus/${month}.yml")
        if (!con.exists()) {
            con.createNewFile()
        } else {
            val configfile = YamlConfiguration.loadConfiguration(con)
            for (i in 0..53) {
                configfile.set("saveinv.$i", e.inventory.getItem(i))
                configfile.save(con)
            }
        }
    }

    fun help(p: CommandSender) {
        p.sendMessage("§d§l====${prefix}§d§l====")
        p.sendMessage("§6/mlb set [month/月] 報酬のセット")
        p.sendMessage("§6/mlb reload コンフィグのリロード")
        p.sendMessage("§6/mlb help ヘルプの表示")
        p.sendMessage("§d============================")
        return
    }

    fun openbonusinventory(p:Player,inv: Inventory){
        val select = ItemStack(Material.CLOCK)
        val name = select.itemMeta
        name.setDisplayName("§4§lアイテムを取得")
        select.itemMeta = name
        inv.setItem(47, select)
        inv.setItem(48, select)
        inv.setItem(49, select)
        inv.setItem(50, select)
        inv.setItem(51, select)
        p.openInventory(inv)
    }

    fun openSettingInventory(p: Player, month: String) {
        val inv = Bukkit.createInventory(null, 54, "${Utility.prefix}${month}月のボーナス報酬設定")
        val one = ItemStack(Material.RED_STAINED_GLASS_PANE)
        val two = ItemStack(Material.BLUE_STAINED_GLASS_PANE)
        val three = ItemStack(Material.PINK_STAINED_GLASS_PANE)
        val four = ItemStack(Material.LIME_STAINED_GLASS_PANE)
        val five = ItemStack(Material.CYAN_STAINED_GLASS_PANE)
        val six = ItemStack(Material.PURPLE_STAINED_GLASS_PANE)
        val seven = ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE)
        val eight = ItemStack(Material.ORANGE_STAINED_GLASS_PANE)
        val nine = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        val ten = ItemStack(Material.YELLOW_STAINED_GLASS_PANE)
        val eleven = ItemStack(Material.BROWN_STAINED_GLASS_PANE)
        val twelve = ItemStack(Material.GREEN_STAINED_GLASS_PANE)
        val select = ItemStack(Material.BARRIER)
        val name = select.itemMeta
        name.setDisplayName("§4§l決定し保存")
        select.itemMeta = name
        if (month == "1") {
            for (num in 0..53) {
                if (num == 0 || num == 1 || num == 7 || num == 8) {
                    inv.setItem(num, one)
                    inv.setItem(num + 9, one)
                    inv.setItem(num + 18, one)
                    inv.setItem(num + 27, one)
                    inv.setItem(num + 36, one)
                    inv.setItem(num + 45, one)
                }
            }
        }
        if (month == "2") {
            for (num in 0..53) {
                if (num == 0 || num == 1 || num == 7 || num == 8) {
                    inv.setItem(num, two)
                    inv.setItem(num + 9, two)
                    inv.setItem(num + 18, two)
                    inv.setItem(num + 27, two)
                    inv.setItem(num + 36, two)
                    inv.setItem(num + 45, two)
                }
            }
        }
        if (month == "3") {
            for (num in 0..53) {
                if (num == 0 || num == 1 || num == 7 || num == 8) {
                    inv.setItem(num, three)
                    inv.setItem(num + 9, three)
                    inv.setItem(num + 18, three)
                    inv.setItem(num + 27, three)
                    inv.setItem(num + 36, three)
                    inv.setItem(num + 45, three)
                }
            }
        }
        if (month == "4") {
            for (num in 0..53) {
                if (num == 0 || num == 1 || num == 7 || num == 8) {
                    inv.setItem(num, four)
                    inv.setItem(num + 9, four)
                    inv.setItem(num + 18, four)
                    inv.setItem(num + 27, four)
                    inv.setItem(num + 36, four)
                    inv.setItem(num + 45, four)
                }
            }
        }
        if (month == "5") {
            for (num in 0..53) {
                if (num == 0 || num == 1 || num == 7 || num == 8) {
                    inv.setItem(num, five)
                    inv.setItem(num + 9, five)
                    inv.setItem(num + 18, five)
                    inv.setItem(num + 27, five)
                    inv.setItem(num + 36, five)
                    inv.setItem(num + 45, five)
                }
            }
        }
        if (month == "6") {
            for (num in 0..53) {
                if (num == 0 || num == 1 || num == 7 || num == 8) {
                    inv.setItem(num, six)
                    inv.setItem(num + 9, six)
                    inv.setItem(num + 18, six)
                    inv.setItem(num + 27, six)
                    inv.setItem(num + 36, six)
                    inv.setItem(num + 45, six)
                }
            }
        }
        if (month == "7") {
            for (num in 0..53) {
                if (num == 0 || num == 1 || num == 7 || num == 8) {
                    inv.setItem(num, seven)
                    inv.setItem(num + 9, seven)
                    inv.setItem(num + 18, seven)
                    inv.setItem(num + 27, seven)
                    inv.setItem(num + 36, seven)
                    inv.setItem(num + 45, seven)
                }
            }
        }
        if (month == "8") {
            for (num in 0..53) {
                if (num == 0 || num == 1 || num == 7 || num == 8) {
                    inv.setItem(num, eight)
                    inv.setItem(num + 9, eight)
                    inv.setItem(num + 18, eight)
                    inv.setItem(num + 27, eight)
                    inv.setItem(num + 36, eight)
                    inv.setItem(num + 45, eight)
                }
            }
        }
        if (month == "9") {
            for (num in 0..53) {
                if (num == 0 || num == 1 || num == 7 || num == 8) {
                    inv.setItem(num, nine)
                    inv.setItem(num + 9, nine)
                    inv.setItem(num + 18, nine)
                    inv.setItem(num + 27, nine)
                    inv.setItem(num + 36, nine)
                    inv.setItem(num + 45, nine)
                }
            }
        }
        if (month == "10") {
            for (num in 0..53) {
                if (num == 0 || num == 1 || num == 7 || num == 8) {
                    inv.setItem(num, ten)
                    inv.setItem(num + 9, ten)
                    inv.setItem(num + 18, ten)
                    inv.setItem(num + 27, ten)
                    inv.setItem(num + 36, ten)
                    inv.setItem(num + 45, ten)
                }
            }
        }
        if (month == "11") {
            for (num in 0..53) {
                if (num == 0 || num == 1 || num == 7 || num == 8) {
                    inv.setItem(num, eleven)
                    inv.setItem(num + 9, eleven)
                    inv.setItem(num + 18, eleven)
                    inv.setItem(num + 27, eleven)
                    inv.setItem(num + 36, eleven)
                    inv.setItem(num + 45, eleven)
                }

            }
        }
        if (month == "12") {
            for (num in 0..53) {
                if (num == 0 || num == 1 || num == 7 || num == 8) {
                    inv.setItem(num, twelve)
                    inv.setItem(num + 9, twelve)
                    inv.setItem(num + 18, twelve)
                    inv.setItem(num + 27, twelve)
                    inv.setItem(num + 36, twelve)
                    inv.setItem(num + 45, twelve)
                }
            }
        }
        inv.setItem(47, select)
        inv.setItem(48, select)
        inv.setItem(49, select)
        inv.setItem(50, select)
        inv.setItem(51, select)
        p.openInventory(inv)
    }
}
