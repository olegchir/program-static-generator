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
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
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
    public String pConfSelector = "2018spb";
    public String REPLACEMENTS_DIR_PATH = pWorkDir + "/replacements";
    public String pTemplate = "default";
    public int imageNum = 0;
    public static boolean DONT_CACHE = false;

    public CDAClient contentful = null;
    public Parser mdparser = Parser.builder().build();
    HtmlRenderer mdrenderer = HtmlRenderer.builder().build();

    @Override
    public void run(String... args) throws Exception {
        initParams();

        FileUtils.forceMkdir(new File(pWorkDir));
        FileUtils.forceMkdir(new File(REPLACEMENTS_DIR_PATH));

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
        applyWhiteList(talks);
        unmarkdown(talks);
        htmlEscape(talks);

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

        String conference = System.getProperty("conference");
        if (null != conference) {
            pConfSelector = conference;
        } else {
            System.out.println("Using default conference selector");
        }

        String template = System.getProperty("template");
        if (null != template) {
            pTemplate = template;
        } else {
            System.out.println("Using default template");
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

    public String unmarkdown(String src) {
        Node document = mdparser.parse(src);
        String rendered = mdrenderer.render(document);
        return rendered;
    }

    public void htmlEscape(List<Talk> talks) {
        for (Talk talk : talks) {
            talk.htmlEscape();
        }
    }

    public void unmarkdown(List<Talk> talks) {
        for (Talk talk : talks) {
            List<String> descriptions = talk.getDescription();
            List<String> newDescriptions = new ArrayList<>();
            for (String description : descriptions) {
                newDescriptions.add(unmarkdown(description));
            }
            talk.setDescription(newDescriptions);

            List<Speaker> speakers = talk.getSpeakers();
            for (Speaker speaker : speakers) {
                List<String> bios = speaker.getBio();
                List<String> newBios = new ArrayList<>();
                for (String bio : bios) {
                    newBios.add(unmarkdown(bio));
                }
                speaker.setBio(newBios);
            }
        }
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

        for (Talk talk: cache) {
            String baseFileName = REPLACEMENTS_DIR_PATH + File.separator + talk.getId();

            String descriptionFilename = baseFileName + "-description.md";
            if (new File(descriptionFilename).exists()) {
                String description = new String(Files.readAllBytes(Paths.get(descriptionFilename)));
                talk.setDescription(Collections.singletonList(description));
            }

            List<Speaker> speakers = talk.getSpeakers();

            int curr = 1;
            for (Speaker speaker : speakers) {
                String speakerFilename = baseFileName + "-bio-"+Integer.toString(curr)+".md";
                if (new File(speakerFilename).exists()) {
                    String bio = new String(Files.readAllBytes(Paths.get(speakerFilename)));
                    speaker.setBio(Collections.singletonList(bio));
                }
                curr++;
            }
        }


        for (Talk replacement: replacements) {
            for (Talk cacheItem: cache) {
                if (replacement.getId().equals(cacheItem.getId())) {
                    cacheItem.replaceWith(replacement);
                }
            }
        }
    }

    public String getWhiteListFileName() {
        return pWorkDir + File.separator + "whitelist.txt";
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

    public void applyWhiteList(List<Talk> cache) throws IOException {
        if (!Files.exists(Paths.get(getWhiteListFileName()))) {
            return;
        }

        List<String> whitelist = Files.lines(Paths.get(getWhiteListFileName())).collect(Collectors.toList());
        if (null == whitelist || whitelist.size() < 1) {
            return;
        }

        for (Iterator<Talk> iterator = cache.iterator(); iterator.hasNext();) {
            Talk cacheItem = iterator.next();
            boolean found = false;
            for (String whiteword: whitelist) {
                if (cacheItem.getId().equals(whiteword)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                iterator.remove();
            }
        }
    }

    public String getCacheFileName() {
        return pWorkDir + File.separator + "cache.json";
    }

    public void saveResult(String template) throws IOException {
        String filename = String.format("%s%s.html", pWorkDir + File.separator, "result");
        Path path = Paths.get(filename);
        Files.deleteIfExists(path);
        Files.write(path, template.getBytes());
    }

    public String getTemplateFileName() {
        return pWorkDir + File.separator + pTemplate + ".vm";
    }

    public String renderTemplate(List<Talk> talks) throws IOException, ParseException {

        Map<Integer, List<Talk>> dayToTalk = StreamEx.of(talks)
                .sorted(Comparator.comparing(Talk::getTime))
                .groupingBy(Talk::getTalkDay);


        File tfile = new File(getTemplateFileName());
        String content = new String(Files.readAllBytes(tfile.toPath()));


        VelocityEngine ve = new VelocityEngine();
        ve.init();

        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        StringReader reader = new StringReader(content);
        SimpleNode node = runtimeServices.parse(reader, "Template");
        Template template = new Template();
        template.setRuntimeServices(runtimeServices);
        template.setData(node);
        template.initDocument();

        VelocityContext context = new VelocityContext();
        context.put("dayToTalk", dayToTalk);
        context.put("header",  getHeader());
        context.put("footer", getFooter());

        StringWriter writer = new StringWriter();
        template.merge( context, writer );

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

    public List<String> extractFieldIds(CDAEntry src) {
        return src.contentType().fields().stream().map(CDAField::id).collect(Collectors.toList());
    }

    public <T> T findSynonym(CDAEntry src, Class<T> type, String... synonyms) {
        List<String> ids = extractFieldIds(src);
        for (String synonym : synonyms) {
            if (ids.contains(synonym)) {
                return src.getField(synonym);
            }
        }
        throw new RuntimeException(String.format("no sysnonyms parsed from: %s, lookup list: %s", src.toString(), Arrays.toString(synonyms)));
    }

    public <T> List<T> findSynonymList(CDAEntry src, Class<T> elementType, String... synonyms) {
        List<String> ids = extractFieldIds(src);
        for (String synonym : synonyms) {
            if (ids.contains(synonym)) {
                return src.getField(synonym);
            }
        }
        throw new RuntimeException(String.format("no sysnonyms parsed from: %s, lookup list: %s", src.toString(), Arrays.toString(synonyms)));
    }

    public List<Talk> findTalks() {
        List<Talk> talks = new ArrayList<>();
        int currTalkNumber = -1;

        try {
            CDAArray ctalks = null;

            try {
                ctalks = contentful.fetch(CDAEntry.class)
                                .withContentType("talks")
                                .where("fields.conference[in]",pConfSelector)
                                .all();
            } catch (Exception e) {
                //It's OK here
            }

            if (null == ctalks) {
                ctalks = contentful.fetch(CDAEntry.class)
                        .withContentType("talks")
                        .where("fields.conferences[in]",pConfSelector)
                        .all();
            }

            if (null == ctalks) {
                System.out.println("Can't select initial dataset");
            }

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
                talk.setName(findSynonym(ctalk, String.class, "name", "title"));
                talk.setDescription(Collections.singletonList(ctalk.getField("long")));
                talk.setConferences(findSynonymList(ctalk, String.class, "conferences", "conference"));

                talk.setUrl(String.format("%s/%s", pUrl, ctalk.id().toLowerCase()));

                String timeString = findSynonym(ctalk, String.class, "trackTime", "talkTime");
                talk.setTimeString(timeString);
                talk.setTime(parseDateTime(timeString));

                Double talkDay = findSynonym(ctalk, Double.class, "talkDay", "trackDay", "day");
                talk.setTalkDay(talkDay.intValue());

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
