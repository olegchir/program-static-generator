package com.olegchir.jug.site.parser.jbreak2018parser;

import com.contentful.java.cda.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;
import one.util.streamex.StreamEx;
import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AppRunner implements CommandLineRunner {
    public String pUrl = "https://dotnext-piter.ru/2018/spb/talks";
    public String pWorkDir = "/Users/olegchir/tmp/jpoint";
    public int imageNum = 0;
    public static boolean DONT_CACHE = false;

    public CDAClient contentful = null;

    @Override
    public void run(String... args) throws Exception {
        initParams();

        FileUtils.forceMkdir(new File(pWorkDir));

        List<Talk> talks;
        if (!cacheExists() || DONT_CACHE) {
            System.out.println("Extracting talks from the Internets");
            talks = findTalks();
            saveCache(talks);
        } else {
            System.out.println("Extracting talks from cache");
            talks = loadCache();
        }

        applyReplacements(talks);
        applyStopList(talks);

        String result = renderTemplate(talks);
        saveResult(result);

        System.exit(0);
    }

    public void initParams() {
        String url = System.getProperty("url");
        if (null != url) {
            pUrl = url;
        } else {
            System.out.println("Using default URL");
        }

        String workdir = System.getProperty("workdir");
        if (null != workdir) {
            pWorkDir = workdir;
        } else {
            System.out.println("Using default working directory");
        }

        String contentfulSpace = System.getProperty("contentfulSpace");
        String contentfulToken = System.getProperty("contentfulToken");

        contentful = CDAClient.builder()
                .setSpace(contentfulSpace)
                .setToken(contentfulToken)
                .build();
    }

    public boolean cacheExists() {
        return Files.exists(Paths.get(getCacheFileName()));
    }

    public void saveCache(List<Talk> talks) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        writer.writeValue(new File(getCacheFileName()), talks);
    }

    public List<Talk> loadCache() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Talk> talks = mapper.readValue(new File(getCacheFileName()), new TypeReference<List<Talk>>(){});
        return talks;
    }

    public List<Talk> loadReplacements() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Talk> talks = mapper.readValue(new File(getReplatecementsFileName()),
                new TypeReference<List<Talk>>(){});
        return talks;
    }

    public String getReplatecementsFileName() {
        return pWorkDir + File.separator + "replacements.json";
    }

    public void applyReplacements(List<Talk> cache) throws IOException {
        if (!Files.exists(Paths.get(getReplatecementsFileName()))) {
            return;
        }
        List<Talk> replacements = loadReplacements();
        for (Talk replacement: replacements) {
            for (Talk cacheItem: cache) {
                if (replacement.getId().equals(cacheItem.getId())) {
                    cacheItem.replaceWith(replacement);
                }
            }
        }
    }

    public String getStopListFileName() {
        return pWorkDir + File.separator + "stoplist.txt";
    }

    public void applyStopList(List<Talk> cache) throws IOException {
        if (!Files.exists(Paths.get(getStopListFileName()))) {
            return;
        }

        List<String> stoplist = Files.lines(Paths.get(getStopListFileName())).collect(Collectors.toList());
        for (Iterator<Talk> iterator = cache.iterator(); iterator.hasNext();) {
            Talk cacheItem = iterator.next();
            for (String stopword: stoplist) {
                if (cacheItem.getId().equals(stopword)) {
                    iterator.remove();
                }
            }
        }
    }

    public String getCacheFileName() {
        return pWorkDir + File.separator + "cache.json";
    }

    public void saveResult(String template) throws IOException {
        String filename = String.format("%s%s.md", pWorkDir + File.separator, "result");
        Path path = Paths.get(filename);
        Files.deleteIfExists(path);
        Files.write(path, template.getBytes());
    }

    public String getTemplateFileName() {
        return pWorkDir + File.separator + "template.vm";
    }

    public String renderTemplate(List<Talk> talks) throws IOException, ParseException {

        Map<Integer, List<Talk>> dayToTalk = StreamEx.of(talks)
                .sorted(Comparator.comparing(Talk::getTime))
                .groupingBy(Talk::getTalkDay);


        File tfile = new File(getTemplateFileName());
        String content = new String(Files.readAllBytes(tfile.toPath()));
         /*  first, get and initialize an engine  */
        VelocityEngine ve = new VelocityEngine();
        ve.init();
        /*  next, get the Template  */
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        StringReader reader = new StringReader(content);
        SimpleNode node = runtimeServices.parse(reader, "Template");
        Template template = new Template();
        template.setRuntimeServices(runtimeServices);
        template.setData(node);
        template.initDocument();
        /*  create a context and add data */
        VelocityContext context = new VelocityContext();
        context.put("dayToTalk", dayToTalk);
        context.put("header",  getHeader());
        context.put("footer", getFooter());

        /* now render the template into a StringWriter */
        StringWriter writer = new StringWriter();
        template.merge( context, writer );
        /* show the World */
        return writer.toString();
    }

    public String getHeaderFileName() {
        return pWorkDir + File.separator + "header.md";
    }

    public String getHeader() throws IOException {
        List<String> content = Files.lines(Paths.get(getHeaderFileName())).collect(Collectors.toList());
        return content.stream().collect(Collectors.joining("\n"));
    }

    public String getFooterFileName() {
        return pWorkDir + File.separator + "footer.md";
    }

    public String getFooter() throws IOException {
        List<String> content = Files.lines(Paths.get(getFooterFileName())).collect(Collectors.toList());
        return content.stream().collect(Collectors.joining("\n"));
    }

    public List<Talk> findTalks() {
        List<Talk> talks = new ArrayList<>();
        int currTalkNumber = -1;

        try {
            CDAArray ctalks = contentful.fetch(CDAEntry.class)
                            .withContentType("talks")
                            .where("fields.conference[in]","2018spb")
                            .all();

            for (CDAResource cres : ctalks.items()) {

                CDAEntry ctalk = null;
                if (cres instanceof CDAEntry) {
                    ctalk = (CDAEntry) cres;
                } else {
                    System.out.println(String.format("Resource of invalid type: %s", cres.getClass().getCanonicalName()));
                }

                currTalkNumber++;

                Talk talk = new Talk();
                talk.setId(ctalk.id());
                talk.setName(ctalk.getField("name"));
                talk.setDescription(Collections.singletonList(ctalk.getField("long")));
                talk.setConferences(ctalk.getField("conference"));
                talk.setUrl(String.format("%s/%s", pUrl, ctalk.id().toLowerCase()));

                String timeString = ctalk.getField("trackTime");
                talk.setTimeString(timeString);
                talk.setTime(parseDateTime(timeString));


                Double talkDay = ctalk.getField("talkDay");
                if (null != talkDay) {
                    talk.setTalkDay(talkDay.intValue());
                } else {
                    System.out.println(String.format("Talk without a day: %s", debugTalk(talk)));
                }

                talk.setSpeakers(new ArrayList<>());
                ArrayList<CDAEntry> speakers = ctalk.getField("speakers");

                if (null != speakers) {
                    for (CDAEntry cspeaker : speakers) {
                        Speaker speaker = new Speaker();
                        speaker.setSpeaker(cspeaker.getField("name"));
                        speaker.setBio(Collections.singletonList(cspeaker.getField("bio")));
                        speaker.setCompany(cspeaker.getField("company"));

                        CDAAsset photo = cspeaker.getField("photo");
                        Map<String, String> photoFile = photo.getField("file");
                        String photoUrl = photoFile.get("url");
                        speaker.setImageUrl(photoUrl);

                        talk.getSpeakers().add(speaker);
                    }
                } else {
                    System.out.println(String.format("Talk without speakers: %s",debugTalk(talk)));
                }

                talks.add(talk);

                System.out.println(String.format("new talk (%s) (day %d)",
                        talk.getSpeakers().size() > 0 ? talk.getSpeakers().get(0).getSpeaker() : "no speakers yet",
                        talk.getTalkDay())
               );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println(String.format("Total talks: %d", currTalkNumber));
        return talks;
    }


    String debugTalk(Talk talk) {
        return String.format("%s (%s)", talk.getId(), talk.getName());
    }

    public Date parseDateTime(String stringTime) {
        String[] splitTime = stringTime.split(":");
        int hours = Integer.parseInt(splitTime[0]);
        int minutes = Integer.parseInt(splitTime[1]);

        DateTimeZone zoneMoscow = DateTimeZone.forID( "Europe/Moscow" );
        DateTime nowMoscow = DateTime.now( zoneMoscow );
        DateTime slotTime = nowMoscow.withTime(hours, minutes, 0, 0);
        Date slotDate = slotTime.toDate();

        return slotDate;
    }
}
