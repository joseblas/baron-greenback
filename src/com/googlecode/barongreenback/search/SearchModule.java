package com.googlecode.barongreenback.search;

import com.googlecode.barongreenback.shared.pager.Pager;
import com.googlecode.barongreenback.shared.pager.RequestPager;
import com.googlecode.barongreenback.shared.sorter.Sorter;
import com.googlecode.lazyrecords.parser.ParametrizedParser;
import com.googlecode.lazyrecords.parser.ParserParameters;
import com.googlecode.lazyrecords.parser.PredicateParser;
import com.googlecode.lazyrecords.parser.StandardParser;
import com.googlecode.utterlyidle.Resources;
import com.googlecode.utterlyidle.modules.Module;
import com.googlecode.utterlyidle.modules.ModuleDefiner;
import com.googlecode.utterlyidle.modules.ModuleDefinitions;
import com.googlecode.utterlyidle.modules.RequestScopedModule;
import com.googlecode.utterlyidle.modules.ResourcesModule;
import com.googlecode.yadic.Container;

import static com.googlecode.utterlyidle.annotations.AnnotatedBindings.annotatedClass;

public class SearchModule implements ResourcesModule, RequestScopedModule, ModuleDefiner {
    public Module addResources(Resources resources) {
        resources.add(annotatedClass(SearchResource.class));
        return this;
    }

    public Module addPerRequestObjects(Container container) throws Exception {
		container.add(Pager.class, RequestPager.class);
        container.add(Sorter.class, Sorter.class);
        container.add(PredicateParser.class, StandardParser.class);
        container.decorate(PredicateParser.class, ParametrizedParser.class);
        container.add(PredicateBuilder.class);
        container.add(ParserParameters.class);
        return this;
    }

    public Module defineModules(ModuleDefinitions moduleDefinitions) throws Exception {
        moduleDefinitions.addRequestModule(ParserParametersModule.class);
        return this;
    }
}
