package point.rar.domain.model;

import point.rar.domain.model.PageLinks;

import java.util.Map;

record QueryLinks(Map<String, PageLinks> pages){
}
