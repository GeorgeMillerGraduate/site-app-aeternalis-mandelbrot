package com.jenga.aeternalis.persistence;
import com.jenga.aeternalis.app.StudioDocument;
import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
/** Versioned UTF-8 JSON projects. Full validation precedes replacement of the open document. */
public final class ProjectIO {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private record Project(int schemaVersion,String applicationVersion,StudioDocument document){}
    private ProjectIO(){}
    public static void save(Path path,StudioDocument document)throws IOException {
        Path target=path.toAbsolutePath(),parent=target.getParent();Files.createDirectories(parent);Path temporary=Files.createTempFile(parent,"aeternalis-",".tmp");
        try{
            try(Writer out=Files.newBufferedWriter(temporary)){JSON.toJson(new Project(1,com.jenga.aeternalis.app.BuildInfo.VERSION,document),out);}
            try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ex){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
    public static StudioDocument load(Path path)throws IOException {
        if(Files.size(path)>2_000_000)throw new IOException("Project exceeds the 2 MB parameter-file limit.");
        try(Reader in=Files.newBufferedReader(path)){
            Project project=JSON.fromJson(in,Project.class);
            if(project==null||project.schemaVersion()!=1||project.document()==null)throw new IOException("Unsupported or incomplete Aeternalis project.");
            return project.document();
        }catch(RuntimeException ex){throw new IOException("Invalid project: "+ex.getMessage(),ex);}
    }
}
