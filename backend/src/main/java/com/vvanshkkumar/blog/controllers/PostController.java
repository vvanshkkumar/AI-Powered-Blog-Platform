package com.vvanshkkumar.blog.controllers;

import com.vvanshkkumar.blog.domain.CreatePostRequest;
import com.vvanshkkumar.blog.domain.UpdatePostRequest;
import com.vvanshkkumar.blog.domain.dtos.CreatePostRequestDto;
import com.vvanshkkumar.blog.domain.dtos.PostDto;
import com.vvanshkkumar.blog.domain.dtos.UpdatePostRequestDto;
import com.vvanshkkumar.blog.domain.entities.Post;
import com.vvanshkkumar.blog.domain.entities.User;
import com.vvanshkkumar.blog.mappers.PostMapper;
import com.vvanshkkumar.blog.services.PostService;
import com.vvanshkkumar.blog.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostMapper postMapper;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<PostDto>> getAllPosts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID tagId) {
        List<Post> posts = postService.getAllPosts(categoryId, tagId);
        List<PostDto> postDtos = posts.stream().map(postMapper::toDto).toList();
        return ResponseEntity.ok(postDtos);
    }

    @GetMapping(path = "/drafts")
    public ResponseEntity<List<PostDto>> getDrafts(@RequestAttribute UUID userId) {
        User loggedInUser = userService.getUserById(userId);
        List<Post> draftPosts = postService.getDraftPosts(loggedInUser);
        List<PostDto> postDtos = draftPosts.stream().map(postMapper::toDto).toList();
        return ResponseEntity.ok(postDtos);
    }

    @PostMapping
    public ResponseEntity<PostDto> createPost(
            @Valid @RequestBody CreatePostRequestDto createPostRequestDto,
            @RequestAttribute UUID userId) {
        User loggedInUser = userService.getUserById(userId);
        CreatePostRequest createPostRequest = postMapper.toCreatePostRequest(createPostRequestDto);
        Post createdPost = postService.createPost(loggedInUser, createPostRequest);
        PostDto createdPostDto = postMapper.toDto(createdPost);
        return new ResponseEntity<>(createdPostDto, HttpStatus.CREATED);
    }

    @PutMapping(path = "/{id}")
    public ResponseEntity<PostDto> updatePost(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePostRequestDto updatePostRequestDto,
            @RequestAttribute UUID userId) {
        Post existingPost = postService.getPost(id);
        if (!existingPost.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("You can only edit your own posts");
        }
        UpdatePostRequest updatePostRequest = postMapper.toUpdatePostRequest(updatePostRequestDto);
        Post updatedPost = postService.updatePost(id, updatePostRequest);
        PostDto updatedPostDto = postMapper.toDto(updatedPost);
        return ResponseEntity.ok(updatedPostDto);
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<PostDto> getPost(
            @PathVariable UUID id
    ) {
        Post post = postService.getPost(id);
        PostDto postDto = postMapper.toDto(post);
        return ResponseEntity.ok(postDto);
    }

    @DeleteMapping(path = "/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable UUID id, @RequestAttribute UUID userId) {
        Post existingPost = postService.getPost(id);
        if (!existingPost.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own posts");
        }
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

}
