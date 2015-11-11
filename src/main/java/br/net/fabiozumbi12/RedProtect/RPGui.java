package br.net.fabiozumbi12.RedProtect;

import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class RPGui implements Listener{		

	private String name;
	private int size;
	private ItemStack[] guiItens;
	private Player player;
	private Region region;
	private boolean allowEnchant;

	public RPGui(String name, Player player, Region region,Plugin plugin){
		this.name = name;
		this.player = player;
		this.region = region;
		
		if (region.flags.size() <= 9){
			this.size = 9;
			this.guiItens = new ItemStack[this.size];
		} else
		if (region.flags.size() >= 10 && region.flags.size() <= 18){
			this.size = 18;
			this.guiItens = new ItemStack[this.size];
		} else
		if (region.flags.size() >= 19 && region.flags.size() <= 27){
			this.size = 27;
			this.guiItens = new ItemStack[this.size];
		}
				
		allowEnchant = false;
		String bukkitv = Bukkit.getVersion();
		for (int i = 3; i <= 8; i++){
			RedProtect.logger.debug("Finding bukkit version 1.8."+i);
			if (bukkitv.contains("1.8."+i)){
				allowEnchant = true;
				RedProtect.logger.debug("Found version 1.8."+i);
				break;
			}			
		}	
		
		for (String flag:region.flags.keySet()){
			if (!(region.flags.get(flag) instanceof Boolean)){
				continue;
			}
			if (RedProtect.ph.hasPerm(player, "redprotect.flag."+flag) && Material.getMaterial(RPConfig.getGuiFlagString(flag,"material")) != null && RPConfig.isFlagEnabled(flag)){
				if (flag.equals("pvp") && !RedProtect.plugin.getConfig().getStringList("flags-configuration.enabled-flags").contains("pvp")){
    				continue;
    			}	
				
				int i = RPConfig.getGuiSlot(flag);
				
				if (allowEnchant){
					this.guiItens[i] = removeAttribute(RPConfig.getGuiItemStack(flag));
				} else {
					this.guiItens[i] = RPConfig.getGuiItemStack(flag);					
				}				
				ItemMeta guiMeta = this.guiItens[i].getItemMeta();
				guiMeta.setDisplayName(RPConfig.getGuiFlagString(flag,"name"));
				guiMeta.setLore(Arrays.asList(RPConfig.getGuiString("value")+RPConfig.getGuiString(region.flags.get(flag).toString()),"�0"+flag,RPConfig.getGuiFlagString(flag,"description"),RPConfig.getGuiFlagString(flag,"description1"),RPConfig.getGuiFlagString(flag,"description2")));
				if (allowEnchant){					
					if (this.region.getFlagBool(flag)){
						guiMeta.addEnchant(Enchantment.DURABILITY, 0, true);
					} else {
						guiMeta.removeEnchant(Enchantment.DURABILITY);
					}
					guiMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);	
				}				
				this.guiItens[i].setType(Material.getMaterial(RPConfig.getGuiFlagString(flag,"material")));
				this.guiItens[i].setItemMeta(guiMeta);
				i++;
			}
		}
		
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler
	void onInventoryClose(InventoryCloseEvent event){
		if (event.getInventory().getTitle() != null && event.getInventory().getTitle().equals(this.name)) {
			close();
		}
	}
	
	@EventHandler
	void onDeath(PlayerDeathEvent event){
		if (event.getEntity().getOpenInventory().getTitle().equals(this.name)) {
			close();
		}
	}
	
	@EventHandler
	void onPlayerLogout(PlayerQuitEvent event){
		if (event.getPlayer().getInventory().getTitle().equals(this.name)) {
			close();
		}
	}
	
	@EventHandler
	void onPluginDisable(PluginDisableEvent event){
		RedProtect.logger.debug("Is PluginDisableEvent event.");
		for (Player play:event.getPlugin().getServer().getOnlinePlayers()){
			play.closeInventory();
		}
	}
	
	@EventHandler
	void onInventoryClick(InventoryClickEvent event){	
		if (event.getInventory().getTitle() != null && event.getInventory().getTitle().equals(this.name)){
			event.setCancelled(true);	
			ItemStack item = event.getCurrentItem();
			if (item != null && !item.getType().equals(Material.AIR) && event.getRawSlot() >= 0 && event.getRawSlot() <= this.size-1){
				ItemMeta itemMeta = item.getItemMeta();
				String flag = itemMeta.getLore().get(1).replace("�0", "");
				if (RPConfig.getBool("flags-configuration.change-flag-delay.enable")){
					if (RPConfig.getStringList("flags-configuration.change-flag-delay.flags").contains(flag)){
							if (!RedProtect.changeWait.contains(this.region.getName()+flag)){								
								applyFlag(flag, itemMeta, event);	
								RPUtil.startFlagChanger(this.region.getName(), flag, player);
								return;
							} else {
								RPLang.sendMessage(player,RPLang.get("gui.needwait.tochange").replace("{seconds}", RPConfig.getString("flags-configuration.change-flag-delay.seconds")));	
								return;
							}
						} else {
							applyFlag(flag, itemMeta, event);
							return;
						}					
				} else {
					applyFlag(flag, itemMeta, event);
					return;
				}
			}			
	    }
	}
	
	private ItemStack removeAttribute(ItemStack stack) {
	    // May need to clone this ItemStack as a CraftItemStack
	    ItemStack craftStack = NbtFactory.getCraftItemStack(stack);
	 
	    NbtFactory.fromItemTag(craftStack).put("AttributeModifiers", NbtFactory.createList());
	    return craftStack;
	}
	
	private void applyFlag(String flag, ItemMeta itemMeta, InventoryClickEvent event){
		this.region.setFlag(flag, !this.region.getFlagBool(flag));
		RPLang.sendMessage(player, RPLang.get("cmdmanager.region.flag.set").replace("{flag}", "'"+flag+"'") + " " + this.region.getFlagBool(flag));
		if (allowEnchant){
			if (this.region.getFlagBool(flag)){
				itemMeta.addEnchant(Enchantment.DURABILITY, 0, true);
			} else {
				itemMeta.removeEnchant(Enchantment.DURABILITY);
			}
			itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}								
		itemMeta.setLore(Arrays.asList(RPConfig.getGuiString("value")+RPConfig.getGuiString(region.flags.get(flag).toString()),"�0"+flag,RPConfig.getGuiFlagString(flag,"description"),RPConfig.getGuiFlagString(flag,"description1"),RPConfig.getGuiFlagString(flag,"description2")));
		event.getCurrentItem().setItemMeta(itemMeta);
	}
	
	public void close(){
		this.guiItens = null;
		this.name = null;
		this.player = null;
		this.region = null;
		try {
			this.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public void open(){	
		Inventory inv = Bukkit.createInventory(player, this.size, this.name);
	    inv.setContents(this.guiItens);
	    player.openInventory(inv);	     
	}	
}