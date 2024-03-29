package com.findwise.impl;

import com.findwise.DocumentRepository;
import com.findwise.IndexEntry;
import com.findwise.DocumentSortEngine;
import com.findwise.data.Document;
import com.findwise.SearchEngine;
import com.findwise.utils.DocumentUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class SearchEngineImpl implements SearchEngine {

    private static Logger log = LogManager.getLogger(SearchEngineImpl.class);
    private final DocumentSortEngine sortEngine;
    private final Map<String, Set<IndexEntry>> invertedIndexes = new HashMap<>();
    private final DocumentRepository documentRepository;

    public SearchEngineImpl(DocumentSortEngine sortEngine, DocumentRepository documentRepository) {
        this.sortEngine = sortEngine;
        this.documentRepository = documentRepository;
    }

    @Override
    public void indexDocument(String id, String content) {
        documentRepository.addDocument(id, DocumentUtils.parseDocumentToList(content));
        List<String> contentList = DocumentUtils.parseDocumentToList(content);
        contentList.forEach(word -> addIndex(id, word));
    }

    private void addIndex(String id, String word) {
        word = word.toLowerCase();
        invertedIndexes.putIfAbsent(word, new HashSet<>());
        invertedIndexes.get(word).add(new IndexEntryImpl(id));
    }

    @Override
    public List<IndexEntry> search(String term) {
        if (!DocumentUtils.isSingleTerm(term)) {
            log.warn("Entered term: '{}'. The Search Engine only supports single term searches", term);
            return Collections.emptyList();
        }

        Set<IndexEntry> results = invertedIndexes.get(term);

        if (CollectionUtils.isEmpty(results)) {
            log.info("Term: '{}' not found", term);
            return Collections.emptyList();
        }
        return getScoredIndexEntries(term, results).stream().sorted().toList();

    }

    private Set<IndexEntry> getScoredIndexEntries(String term, Set<IndexEntry> results) {
        List<Document> allDocuments = documentRepository.getAllDocuments();

        for (IndexEntry result : results) {
            Document document = documentRepository.getDocument(result.getId());
            double score = sortEngine.score(document, allDocuments, term);
            result.setScore(score);
        }
        return results;
    }

}
