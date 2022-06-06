
  
# BLCMM plugin introduction

*So you want to make another plugin, eh? One which  gracefully handles the messy object data?  Haha, have I got an API for you!* 
This is a quick reference guide on how to get started on BLCMM plugin development.
We assume you have experience in Java, or at least programming in general, so we will just cover the API provided by our plugin framework.

## Your main plugin class
Your main plugin class will extend `BLCMMPlugin`. Here you provide basic information about your plugin. In the constructor, you indicate if your plugin works for BL2 and/or TPS, as well as optionally provide an author and a version number.
In the overwritten methods, you provide the name of the plugin, and which object classes from the Object Explorer your plugin needs.
If the currently open file in BLCMM is of an unsupported game type, or if some of your required data is not present, the plugin will not be able to launch. This ensures that you don't need to check if the data you need is present, you can assume it is.
The final method you need to implement is one that returns a `JPanel`, which will hold your GUI, which you can design in whichever way you like.

**Make sure your plugin has a no-argument constructor!**
Keep this constructor as low cost as possible in terms of computation, so no interactions with the data library.
Upon booting BLCMM, an object is allocated for each plugin, so the constructors of each plugin directly affect the boot time of BLCMM.

There's three subclasses of `BLCMMPlugin` you can use as a base as well.
First of is `BLCMMFilePlugin`, which provides you with the method `getOutputDir`, which returns a `File` containing a directory your plugin is allowed to write to.
Notice that among other things, a BLCMM plugin is not permitted to write outside of this directory, to provide some security.
This kind of plugin could be used for data analysis, or if you want to export the result of your plugin to a file.
The second extension is `BLCMMModelPlugin`. This is used to directly inject the result of your plugin into the currently open file, bypassing the need to import the result, as is necessary with `BLCMMFilePlugin`.
The third extension is `BLCMMUtilityPlugin`. This is meant for plugins which don't produce output. These plugins will open in a non-modal window, meaning that the user of your plugin will still be able to use the rest of BLCMM while your plugin is open. This is not true for the other two types, these will lock the rest of BLCMM.


## Interacting with the data
The main class you will be using to interact with the data is the `Datamanager` class.
The simplest method available is `DataManager.getDump(String object)`. This will return a `Dump` that holds the object you specify.
Example uses include:
`DataManager.getDump("GD_Weap_Shotgun.Barrel.SG_Barrel_Hyperion_HeartBreaker")` or `DataManager.getDump("WeaponPartDefinition'GD_Weap_Shotgun.Barrel.SG_Barrel_Hyperion_HeartBreaker'")`, which is slightly faster (because the library no longer needs to infer the class), and will give you the `Dump` as you'd get from dumping that object in the Object Explorer.
A `Dump` is a triple consisting of an object name, class and the actual dump, all three being `String`.

If you need to know all objects present of a class, we provide `DataManager.getGetAll(String classname)`.

If you want to manipulate a lot, or all of the objects of a certain class, it is advised to use `DataManager.streamAllDumpsOfClassAndSubclasses(String classname)`, which provides a significant performance increase compared to a construct like the following:
```Java
for(String object : DataManager.getGetAll("WeaponPartDefinition"){
    Dump dump = DataManager.getDump(object);
    //Your plugin logic goes here
}
```
Instead, you'd use
```Java
DataManager.streamAllDumpsOfClassAndSubclasses("WeaponPartDefinition").forEach(dump -> {
    //Your plugin logic goes here
});
```
### Making the data more manageable
Okay, so you have a `Dump`, that's nifty, so now what, how do you work that thing?
In some low-level data analysis cases, or when performance is essential, one might choose to do raw String manipulations oneself.
In most cases however, it is preferred to use the class `BorderlandsObject` to parse your object into something more manageable.
To parse your object, you use `BorderlandsObject.parseObject(dump)` which will return a `BorderlandsObject`.
A `BorderlandsObject` provides methods like `getField`, `getArrayField`, `getStructField`, `getFloatField` and similar ones.
You can now make stuff like the following:
```Java
DataManager.streamAllDumpsOfClassAndSubclasses("WeaponPartDefinition").forEach(dump -> {
    BorderlandsObject myPart = BorderlandsObject.parseObject(dump);
    BorderlandsArray ExternalAttributeEffects = myPart.getArrayField("ExternalAttributeEffects");
});
```
Now, let's say you want to add 20% critical hit damage to all the barrels in the game.
What you could do is the following;
```Java
DataManager.streamAllDumpsOfClassAndSubclasses("WeaponPartDefinition").forEach(dump -> {
    BorderlandsObject myPart = BorderlandsObject.parseObject(dump);
    if ("WP_Barrel".equals(myPart.getField("PartType"))) {
        BorderlandsArray<BorderlandsStruct> EAE = myPart.getArrayField("ExternalAttributeEffects");
        if (EAE == null) {//Some barrels may not touch external attributes, so check for null
            EAE = new BorderlandsArray<>();
        }
        String toAdd = "(AttributeToModify=AttributeDefinition'D_Attributes.GameplayAttributes.PlayerCriticalHitBonus',ModifierType=MT_Scale,BaseModifierValue=(BaseValueConstant=0.200000,BaseValueAttribute=None,InitializationDefinition=None,BaseValueScaleConstant=1.000000))";
        EAE.add(BorderlandsStruct.parseStruct(toAdd));
        System.out.println("set " + myPart.getName() + " ExternalAttributeEffects " + EAE.toString());
    }
});
```
Here you immediately see the class `BorderlandsStruct` in action, which provides similar API as `BorderlandsObject` and `BorderlandsArray`. Scroll through the methods shown by autocomplete and read the javadoc that comes with the plugin framework for more details.
In the example above we use `System.out` to export the result of our mod, which is fine for testing.
Normally you'd want to store it, and export it to a file or to the BLCMM model.

## Outputting your mod
The simplest way to output your mod is to just print it to `System.out` while you create it, copy paste it into a BLCMM edit window, and call it a day.

If your plugin is based on `BLCMMFilePlugin`, you can write whatever output you wish to any `File` which has  `getOutputDir()` as an ancestor.  Note you'll have to provide the user with a button they can use to start the output process.

If your plugin is based on `BLCMMModelPlugin` your plugin class needs to implement `getOutputModel()`. This method will be called upon when the user presses the OK button provided by BLCMM.
The model returned will be inserted in the currently open file in BLCMM.
The `BLCMMModelPlugin` also comes with the `getProgressBar()` method. If your plugin takes a while to execute, make this method return a `JProgressbar` that is updated during the execution of `getOutputModel()`. BLCMM will display the progressbar after the user presses OK, and handle the associated threading.
If your code generation is quick, just let `getProgressBar()` return `null`.

### The pseudo model
The method `getOutputModel()` of `BLCMMModelPlugin` returns a `PCategory`, or a Pseudo Category. This is the root of your pseudo model. The pseudo model is a simplified version of what BLCMM uses, and it's easier to manipulate with code.
A `PCategory` has `PModelElement` children, which are either `PComment` for descriptions, `PCommand` for set commands, `PHotfix` for hotfixes, or `PCategory` for nested categories.
See the bundled javadoc for how to instantiate those respective objects.

## Utilities

If you want access to the currently open file in BLCMM, you can use `BLCMMPlugin.getCurrentlyOpenedBLCMMModel()` to get a copy of the currently open model in BLCMM.
If you're after something specific, analyze this pseudo model however you wish.
In case you want to edit the objects the way they would be after applying the current patch, you'll need to convert the pseudo model.
You can convert the `PCategory` to an `ApplyablePModel` by using the `ApplyablePModel(PCategory myModel)` constructor.
The `ApplyablePModel` object has the method `applyTo(BorderlandsObject object)`, that will change the argument object to represent its state after the current file has been applied to it.

## Hotfixes or game specifics
If your plugin requires hotfixes, you'll need to use the `PHotfix` commands. These are the same as `PCommands`, except the also take a `HotfixType` argument, along with a parameter and a name.
For `HotfixType.PATCH`, leave the parameter `null`.
For `HotfixType.LEVEL`, if it needs to apply in every level, give the parameter the `String` value `"None"`, not `null`.
For `HotfixType.OnDemand`, use the corresponding streaming package as parameter, as a `String`.
To find which streaming package you need, check out the classes `BLCharacter` and `BLVehicle`.
All enum values listed in there have their streaming package stored. If you need the values filtered for just one of the games, you can either find that trough the `PatchType`, or by one of the sub-enums in the aforementioned enums.
A handy function to just find the `Streamable` (superclass of both `BLCharacter` and `BLVehicle`) is the `Streamable.find(String)` method. Feed it an object name, and it'll return the `Streamable` needed for that object to load.

# The SDK
So, you want to do more complicated stuff, and using methods like `BorderlandsObject.getFloatField("MyFieldNameHere")` is getting unwieldy with all the `String` arguments and lackluster type safety?
Well, then you'll want to proceed to use the SDK, which will make your code more typesafe, and easier to read.

The SDK has a Java class for all classes used in the game, as well as for all structs.
These two snippets of code do the same thing;
```Java
BorderlandsObject obj = BorderlandsObject.parseObject(DataManager.getDump("GD_Weap_Shotgun.A_Weapons.WT_Hyperion_Shotgun"));
float spread = obj.getFloatField("Spread");
```
```Java
WeaponTypeDefinition typedef = WeaponTypeDefinition.getObject("GD_Weap_Shotgun.A_Weapons.WT_Hyperion_Shotgun");
float spread = typedef.Spread;
```
This makes the code much more readable, provides better typesafety, and is shorter.
Almost all fields in the SDK classes are `public` and can be freely edited, and provide accurate `toString` methods. This means that whatever code-based manipulation you do to an object, can easily be converted to a single set command, for use in the plugin output.

Just like the `DataManager.streamAllDumpsOfClass` methods, there's methods that stream SDK versions of these objects as well, having the same performance benefits as the non-SDK variant.
Another way to stream trough all objects is to use the class `ObjectCache` rather than `DataManager`. This class streams and caches all parsed objects. This will improve performance if you need to access the objects at a later time, since it won't need to parse and convert to an SDK object anymore. Do not that this increases memory usage.

Another handy functionality is how references are handled. Wherever the object data refers to another object, it'll be parsed as a `ObjectReference` object in the SDK, which comes with the `getReferencedObject`, giving you the SDK object the reference refers to.
**Note:** if you know that you'll be using `getReferencedObject` a lot, always getting a single object from a certain class, say `ItemPoolDefinition`, it will often yield big performance benefits to run the following snippet:
```Java
ObjectCache.registerClass(ItemPoolDefinition.class);
ObjectCache.getStream(ItemPoolDefinition.class).count();
```
This will first cache all objects, which makes it so that resolving an `ObjectReference.getReferencedObject` takes constant time, rather than linear time on each call.

> :warning: **Warning** When using `getReferencedObject`, you might get a `NullpointerException`. This is caused by faults in the object data. It happens when you have a reference like `Class1'Object1'`, but `Object1` is an instance of `Class2`, instead of `Class1`. This happens in multiple cases in DLC5 data from BL2.
> An example would be `ItemPoolDefinition'GD_Anemone_Side_VaughnPart3.ItemDefs.BD_ArtifactOfPower'` found in `GD_Anemone_Side_VaughnPart3.IO_WashingMachineArtefact:BehaviorProviderDefinition_0.Behavior_SpawnItems_54`, which is an `InventoryBalanceDefinition`, not an `ItemPoolDefinition`.
> Besides this `NullPointerException`, `getReferencedObject` will never return `null`, provided the required data is present, which can be guaranteed trough the `getRequiredDataClasses` method in the main plugin class.

With all that, let's look at some example code.
This is taken from the LootDecertainifier plugin, where we modify the droprate of skins.
```Java
ApplyablePModel model = new ApplyablePModel(BLCMMPlugin.getCurrentlyOpenedBLCMMModel());
DataManager.streamAllObjectsOfClassAndSubclasses(ItemPoolDefinition.class)//Loop over all IPDs
        .map(obj -> model.applyTo(obj))//Take all currently active modifications to IPDs into account
        .filter(pool -> pool.BalancedItems != null)//There's some IPDs which don't have their BalancedItems field set
        .forEach(pool -> {
            for (int i = 0; i < pool.BalancedItems.length; i++) {//We need i later, so no flatmap here
                if (pool.BalancedItems[i].InvBalanceDefinition != ObjectReference.NONE) {//We're only interested in modifiying the leafs of the IPD tree
                    try {
                        InventoryBalanceDefinition ref = pool.BalancedItems[i].InvBalanceDefinition.getReferencedObject();//Get the actual item that drops
                        ObjectReference<WillowInventoryDefinition> inv = ref.InventoryDefinition;
                        if (inv != ObjectReference.NONE && inv.clas.equals("UsableCustomizationItemDefinition")) {
                            double newodds = pool.BalancedItems[i].Probability.BaseValueScaleConstant * skinf;
                            skins.addChild(new PHotfix("set " + pool.getFullyQuantizedName() + " BalancedItems[" + i + "].Probability.BaseValueScaleConstant " + newodds, HotfixType.LEVEL, "None", "decertainifier"));
                        }
                    } catch (NullPointerException e) {
                        System.err.println("Could not find " + pool.BalancedItems[i].InvBalanceDefinition + " in " + pool.getFullyQuantizedName());
                        //This catches one instance, in the DLC 5 data, described in the warning above
                    }
                }
            }
        });
```

Another edge the SDK-style object are superior to the map & string based objects, is that you can take 
Say you have a BaseValueConstant quadruple, or an attribute, or something else you could evaluate to a numerical value. 
You can use the `SDK_Utilities` class to evaluate such quantities, which will recursively handle whatever is found inside.
Some of the things found inside are dependent on things like playthrough, number of players and VH relics equipped.
Those can be changed through `SDK_Utilities.setGLOBAL_SETTINGS(GlobalSettings newSettings)`, which takes a new `GlobalSettings` object as argument.
The default setting is single player endgame with no VH relics equipped.
Even with that said, it'll sometimes fail to resolve a value, if it depends on the current weapon, or skill, or other context.
When it does, it will throw an exception on which `AttributeValueResolver` it has trouble with.
You can then supply a function trough `setExtraVRFunction`, which it will then use to resolve unknown AttributeValueResolvers.