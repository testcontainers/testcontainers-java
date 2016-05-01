package org.testcontainers.containers.traits;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.google.common.collect.ObjectArrays;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.SelfReference;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@RequiredArgsConstructor
public class Link<SELF extends Container<SELF>> implements Trait<SELF> {

    public interface Support<SELF extends Container<SELF>> extends SelfReference<SELF> {

        /**
         * Add a link to another container.
         *
         * @param otherContainer
         * @param alias
         */
        default void addLink(LinkableContainer otherContainer, String alias) {
            self().getTraits().removeIf(trait -> trait instanceof Link && alias.equals(((Link) trait).getAlias()));

            self().with(new Link<>(otherContainer, alias));
        }

        default Map<String, LinkableContainer> getLinkedContainers() {
            Stream<Link> traits = self().getTraits(Link.class);
            return traits.collect(Collectors.toMap(Link::getAlias, Link::getOtherContainer));
        }

        default void setLinkedContainers(Map<String, LinkableContainer> value) {
            self().replaceTraits(Link.class, value.entrySet().stream().map(entry -> new Link(entry.getValue(), entry.getKey())));
        }
    }

    protected final LinkableContainer otherContainer;

    protected final String alias;

    @Override
    public void configure(SELF container, CreateContainerCmd createContainerCmd) {
        com.github.dockerjava.api.model.Link[] links = createContainerCmd.getLinks();

        if (links == null) {
            links = new com.github.dockerjava.api.model.Link[] {};
        }

        createContainerCmd.withLinks(ObjectArrays.concat(links, new com.github.dockerjava.api.model.Link(otherContainer.getContainerName(), alias)));
    }
}
