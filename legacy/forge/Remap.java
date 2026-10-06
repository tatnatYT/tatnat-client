import java.nio.file.Path;
import java.nio.file.Paths;

import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;

/**
 * Remaps the Legacy Fabric jar (intermediary names) to Forge 1.8.9's runtime names (SRG).
 * The mixin extension also renames @Shadow / @Overwrite members, which the refmap doesn't cover.
 *   java -cp tiny-remapper.jar Remap.java <in.jar> <out.jar> <mappings.tiny> <classpath jars...>
 */
public class Remap {
	public static void main(String[] args) throws Exception {
		Path in = Paths.get(args[0]), out = Paths.get(args[1]);
		TinyRemapper remapper = TinyRemapper.newRemapper()
				.withMappings(TinyUtils.createTinyMappingProvider(Paths.get(args[2]), "intermediary", "srg"))
				.extension(new MixinExtension())
				.ignoreConflicts(true)
				.build();
		try (OutputConsumerPath output = new OutputConsumerPath.Builder(out).build()) {
			output.addNonClassFiles(in, NonClassCopyMode.UNCHANGED, remapper);
			remapper.readInputs(in);
			for (int i = 3; i < args.length; i++) remapper.readClassPath(Paths.get(args[i]));
			remapper.apply(output);
		} finally {
			remapper.finish();
		}
	}
}
