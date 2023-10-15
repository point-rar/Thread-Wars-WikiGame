package rar.java.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import rar.java.wiki.domain.repository.WikiRepository;
import rar.kotlin.model.BackwardPage;
import rar.kotlin.model.ForwardPage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class ReactorAlgoWikiGameImpl implements WikiGame {


	private record PairPages(ForwardPage forwardPage, BackwardPage backwardPage) {
	}


	private final WikiRepository wikiRepository;

	public ReactorAlgoWikiGameImpl(WikiRepository wikiRepository) {
		this.wikiRepository = wikiRepository;
	}

	@Override
	public List<String> play(String startPageTitle, String endPageTitle, int maxDepth) {
		var visitedForwardPages = new ConcurrentHashMap<String, ForwardPage>();
		var visitedBackwardPages = new ConcurrentHashMap<String, BackwardPage>();

		var startForwardPage = new ForwardPage(startPageTitle, null);
		var endBackwardPage = new BackwardPage(endPageTitle, null);

		PairPages pairPagesResult = Flux.firstWithValue(
						Mono.just(startForwardPage)
								.expand(page -> getLinks(page)
										.map(link -> new ForwardPage(link, page))
										.filter(page2 -> visitedForwardPages.putIfAbsent(page2.getTitle(), page2) == null)
								)
								.filter(page -> visitedBackwardPages.containsKey(page.getTitle()))
								.map(page -> new PairPages(page, visitedBackwardPages.get(page.getTitle()))),


						Mono.just(endBackwardPage)
								.expand(page -> getBackwardLinks(page)
										.map(link -> new BackwardPage(link, page))
										.filter(page2 -> visitedBackwardPages.putIfAbsent(page2.getTitle(), page2) == null)
								)
								.filter(page -> visitedForwardPages.containsKey(page.getTitle()))
								.map(page -> new PairPages(visitedForwardPages.get(page.getTitle()), page))
				)
				.blockFirst();

		return getFinalPathFromForwardAndBackward(pairPagesResult.forwardPage, pairPagesResult.backwardPage);
	}

	private Flux<String> getLinks(ForwardPage page) {
		return Mono.fromCallable(() -> wikiRepository.getLinksByTitle(page.getTitle()))
				.subscribeOn(Schedulers.boundedElastic())
				.flatMapIterable(Function.identity());
	}

	private Flux<String> getBackwardLinks(BackwardPage page) {
		return Mono.fromCallable(() -> wikiRepository.getBacklinksByTitle(page.getTitle()))
				.subscribeOn(Schedulers.boundedElastic())
				.flatMapIterable(Function.identity());
	}

	private List<String> getFinalPathFromForwardAndBackward(
			ForwardPage forwardPage,
			BackwardPage backwardPage
	) {
		var path = new ArrayList<String>();

		var forwardPages = new ArrayList<ForwardPage>();
		var curFwdPage = forwardPage;
		while (curFwdPage != null) {
			forwardPages.add(curFwdPage);
			curFwdPage = curFwdPage.getParentPage();
		}

		Collections.reverse(forwardPages);
		for (var fwdPg : forwardPages) {
			path.add(fwdPg.getTitle());
		}

		var curBwdPage = backwardPage.getChildPage();
		while (curBwdPage != null) {
			path.add(curBwdPage.getTitle());
			curBwdPage = curBwdPage.getChildPage();
		}

		return path;
	}


}
